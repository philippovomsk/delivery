package com.philya.delivery;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.philya.delivery.db.*;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;

import java.io.*;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ExchangeJobService extends JobService implements Runnable {
    public static final SimpleDateFormat docDateFormat = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());

    public static final SimpleDateFormat exchangeDateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault());

    private ExecutorService service;

    private JobParameters parameters;

    public ExchangeJobService() {
        service = Executors.newSingleThreadExecutor();
    }

    @Override
    public boolean onStartJob(JobParameters params) {
        if (!Exchange.canStart()) {
            Log.d("delivery.exchange", "параллельный запуск обмена");
            return true;
        }

        parameters = params;
        service.submit(this);
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        Log.d("delivery.exchange", "Для задачи обмена вызван метод OnStopJob");

        service.shutdownNow();

        Exchange.setRunning(false);
        Exchange.startExchangeJob(this, Exchange.REPEATTIMEOUT);

        return true;
    }

    @Override
    public void run() {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        FTPClient f = new FTPClient();
        try {
            String ftpaddress = pref.getString("ftpaddress", "");
            String basedir = "/";
            if (ftpaddress.indexOf('/') > 0) {
                basedir = ftpaddress.substring(ftpaddress.indexOf('/'));
                ftpaddress = ftpaddress.substring(0, ftpaddress.indexOf('/'));
            }


            Log.d("delivery.exchange", "Подключение к ftp-серверу офиса " + ftpaddress + " пользователь " + pref.getString("ftpuser", ""));

            f.connect(ftpaddress);
            f.login(pref.getString("ftpuser", ""), pref.getString("ftppassword", ""));
            f.enterLocalPassiveMode();
            f.setFileType(FTP.BINARY_FILE_TYPE);

            if (!f.sendNoOp()) {
                throw new IOException("Ошибка подключения к ftp-серверу");
            }

            if (!basedir.isEmpty()) {
                Log.d("delivery.exchange", "Переход в папку " + basedir);
                f.changeWorkingDirectory(basedir);
            }

            sendDataToOffice(f);
            loadDataFromOffice(f);

        } catch (IOException | RuntimeException exc) {
            Log.e("delivery.exchange", "Ошибка обмена с офисом", exc);
        } finally {
            if (f.isConnected()) {
                try {
                    f.disconnect();
                } catch (IOException e) {
                    // do nothing
                }
            }
        }

        Log.d("delivery.exchange", "Завершение обмена с офисом");

        pref.edit().putString("lastexchange", exchangeDateFormat.format(new Date())).apply();

        Exchange.setRunning(false);
        jobFinished(parameters, false);

        Exchange.startExchangeJob(this, Exchange.REPEATTIMEOUT);
    }

    private void sendDataToOffice(FTPClient f) throws IOException {
        Log.d("delivery.exchange", "Отправка подтверждений в офис");

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String currentDriverId = preferences.getString("currentDriverId", "drivernotselect");

        Db db = ((DeliveryApp) getApplication()).getDatabase();

        List<Round> forsend = db.roundDocDAO().getForSend(currentDriverId);
        if(forsend.size() == 0) {
            Log.d("delivery.exchange", "Нет данных для отправки");
            return;
        }

        f.sendNoOp();

        List<Map<String, Object>> roundComplete = new ArrayList<>();
        for(Round doc : forsend) {
            Map<String, Object> dc = new HashMap<>();
            dc.put("id", doc.id);
            dc.put("complete", doc.complete);
            roundComplete.add(dc);
        }

        f.sendNoOp();

        GsonBuilder gsonBuilder = new GsonBuilder();
        Gson gson = gsonBuilder.create();

        Map<String, Object> resobj = new HashMap<>();
        resobj.put("roundComplete", roundComplete);

        String resstr = gson.toJson(resobj);

        f.sendNoOp();

        String prevSendStr = preferences.getString("prevSendData", "");
        if(resstr.equals(prevSendStr)) {
            Log.d("delivery.exchange", "Нет изменений с последней отправки");
            return;
        }

        preferences.edit().putString("prevSendData", resstr).apply();

        int officeExchangeNumber = preferences.getInt("officeExchangeNumber", 0) + 1;

        String[] filesindir = f.listNames();
        if (filesindir == null) {
            filesindir = new String[0];
        }
        List<String> filesindirlist = Arrays.asList(filesindir);
        while(filesindirlist.contains("r" + currentDriverId + "_" + String.format("%010d", officeExchangeNumber) + ".json")) {
            officeExchangeNumber++;
        }
        preferences.edit().putInt("officeExchangeNumber", officeExchangeNumber).apply();

        File outfile = File.createTempFile("fromapp", "json", getCacheDir());

        try (Writer w = new FileWriter(outfile)) {
            w.write(resstr);
        }

        try (FileInputStream fi = new FileInputStream(outfile)) {
            if (!f.storeFile("r" + currentDriverId + "_" + String.format("%010d", officeExchangeNumber) + ".json", fi)) {
                throw new IOException("Не удалось отправить файл r" + currentDriverId + "_" + String.format("%010d", officeExchangeNumber) + ".json");
            }
        } finally {
            outfile.delete();
        }

        Log.d("delivery.exchange", "Отправлен файл с данными " + officeExchangeNumber);
    }

    private <T extends EntityId> void saveDataToDb(ExchangeDAO<T> dao, List<T> newItems) {
        Map<String, T> m = new HashMap<>();
        for (T i : newItems) {
            m.put(i.getId(), i);
        }

        List<T> fordel = new ArrayList<>();
        List<T> forupd = new ArrayList<>();

        List<T> oldItems = dao.getAll();
        for (T i : oldItems) {
            if (m.containsKey(i.getId())) {
                T ni = m.get(i.getId());
                if(ni instanceof RoundDoc) {
                    ((RoundDoc) ni).head.complete = ((RoundDoc) i).head.complete;
                }

                forupd.add(ni);
                m.remove(i.getId());
            } else {
                fordel.add(i);
            }
        }

        List<T> forins = new ArrayList<>(m.values());

        dao.deleteAll(fordel);
        dao.updateAll(forupd);
        dao.insertAll(forins);
    }

    private void loadDataFromOffice(FTPClient f) throws IOException {
        String[] filesindir = f.listNames("d.json");
        if (filesindir == null || filesindir.length == 0) {
            Log.d("delivery.exchange", "В ftp папке нет d.json");
            return;
        }

        File localcopy = File.createTempFile("fromoffice", "json", getCacheDir());
        OutputStream output = new FileOutputStream(localcopy);

        Log.d("delivery.exchange", "Начало закачки файла");
        if (!f.retrieveFile("d.json", output)) {
            output.close();
            localcopy.delete();

            throw new IOException("Не удалось скачать файл d.json");
        }
        output.close();
        Log.d("delivery.exchange", "Конец закачки файла");

        try (FileReader fr = new FileReader(localcopy);
             JsonReader jsonReader = new JsonReader(fr)) {

            GsonBuilder gsonBuilder = new GsonBuilder();
            gsonBuilder.registerTypeAdapter(Date.class, new DateDeserializer());
            Gson gson = gsonBuilder.create();

            Db database = ((DeliveryApp) getApplication()).getDatabase();

            database.getOpenHelper().getWritableDatabase().beginTransactionNonExclusive();
            try {
                jsonReader.beginObject();
                while (jsonReader.hasNext()) {
                    String name = jsonReader.nextName();

                    f.sendNoOp();

                    if (name.equals("driver")) {
                        Type driversType = new TypeToken<List<Driver>>() {
                        }.getType();
                        List drivers = gson.fromJson(jsonReader, driversType);

                        f.sendNoOp();

                        database.beginTransaction();
                        saveDataToDb(database.driverDAO(), drivers);
                        database.setTransactionSuccessful();
                        database.endTransaction();

                        Log.d("delivery.exchange", "Загрузили водителей");
                    } else if (name.equals("round")) {
                        Type roundType = new TypeToken<List<RoundDoc>>() {
                        }.getType();
                        List roundDocs = gson.fromJson(jsonReader, roundType);

                        f.sendNoOp();

                        database.beginTransaction();
                        saveDataToDb(database.roundDocDAO(), roundDocs);
                        database.setTransactionSuccessful();
                        database.endTransaction();

                        Log.d("delivery.exchange", "Загрузили рейсы");
                    } else {
                        Log.e("delivery.exchange", "Неизвестное поле " + name);
                        jsonReader.skipValue();
                    }
                }
                jsonReader.endObject();

                database.setTransactionSuccessful();
            } catch (RuntimeException exc) {
                Log.e("delivery.exchange", "Ошибка парсинга json", exc);
                throw exc;
            } finally {
                database.endTransaction();
            }
        }

        localcopy.delete();
    }

}
