package com.philya.delivery;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.philya.delivery.db.Db;
import com.philya.delivery.db.RoundDoc;
import io.reactivex.Completable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Action;
import io.reactivex.observers.DisposableCompletableObserver;
import io.reactivex.observers.DisposableSingleObserver;
import io.reactivex.schedulers.Schedulers;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;

import java.io.*;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Callable;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static com.philya.delivery.ExchangeJobService.docDateFormat;

public class FreeRoundDocActivity extends AppCompatActivity {

    private RoundDoc doc;

    private CompositeDisposable rxDisposable;

    protected ConstraintLayout head;

    private TextView numberEdit;

    private TextView dateEdit;

    private TextView carLabel;

    private TextView carEdit;

    private TextView fromEdit;

    private CheckBox completed;

    private TextView weight;

    private TextView contractPriceLabel;

    private TextView contractPrice;

    private RoundDocRowsAdapter adapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rounddoc);
        setTitle(R.string.freeRoundDocTitle);

        rxDisposable = new CompositeDisposable();

        if (savedInstanceState != null && savedInstanceState.containsKey("doc")) {
            doc = (RoundDoc) savedInstanceState.getSerializable("doc");
        } else {
            doc = (RoundDoc) getIntent().getExtras().get("doc");
        }

        head = (ConstraintLayout) findViewById(R.id.head);
        numberEdit = (TextView) findViewById(R.id.numberEdit);
        dateEdit = (TextView) findViewById(R.id.dateEdit);
        carLabel = findViewById(R.id.carLabel);
        carEdit = (TextView) findViewById(R.id.carEdit);
        fromEdit = (TextView) findViewById(R.id.fromEdit);
        completed = (CheckBox) findViewById(R.id.completed);
        weight = (TextView) findViewById(R.id.weight);
        contractPriceLabel = findViewById(R.id.contractPriceLabel);
        contractPrice = findViewById(R.id.contractPrice);

        RecyclerView rounddocrows = (RecyclerView) findViewById(R.id.rounddocrows);

        adapter = new RoundDocRowsAdapter(this);
        rounddocrows.setAdapter(adapter);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putSerializable("doc", doc);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onResume() {
        super.onResume();

        numberEdit.setText(doc.head.number);
        dateEdit.setText(docDateFormat.format(doc.head.date));
        fromEdit.setText(doc.head.from);
        completed.setChecked(doc.head.complete);
        weight.setText(String.format(Locale.getDefault(), "%d кг", doc.head.weight));
        contractPrice.setText(String.format(Locale.getDefault(), "%d-00", doc.head.contractPrice));

        carLabel.setVisibility(GONE);
        carEdit.setVisibility(GONE);
        completed.setVisibility(GONE);

        adapter.setRoundDoc(doc);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        rxDisposable.clear();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.freerounddocactivitymenu, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        if (item.getItemId() == R.id.getRound) {
            item.setEnabled(false);

            rxDisposable.add(Single.fromCallable(new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                    String currentDriverId = preferences.getString("currentDriverId", "drivernotselect");

                    RoundLock lock = new RoundLock();
                    lock.driverId = currentDriverId;
                    lock.roundId = doc.head.id;

                    GsonBuilder gsonBuilder = new GsonBuilder();
                    Gson gson = gsonBuilder.create();

                    String resstr = gson.toJson(lock) + '\n';

                    File outfile = File.createTempFile("lockround", "json", getCacheDir());

                    try (Writer w = new FileWriter(outfile)) {
                        w.write(resstr);
                    }

                    FTPClient f = new FTPClient();
                    try {
                        String ftpaddress = preferences.getString("ftpaddress", "");
                        String basedir = "/";
                        if (ftpaddress.indexOf('/') > 0) {
                            basedir = ftpaddress.substring(ftpaddress.indexOf('/'));
                            ftpaddress = ftpaddress.substring(0, ftpaddress.indexOf('/'));
                        }

                        f.connect(ftpaddress);
                        f.login(preferences.getString("ftpuser", ""), preferences.getString("ftppassword", ""));
                        f.enterLocalPassiveMode();
                        f.setFileType(FTP.BINARY_FILE_TYPE);

                        if (!f.sendNoOp()) {
                            throw new IOException("Ошибка подключения к ftp-серверу");
                        }

                        if (!basedir.isEmpty()) {
                            Log.d("delivery.freerounddoc", "Переход в папку " + basedir);
                            f.changeWorkingDirectory(basedir);
                        }

                        String lockFileName = "lr" + doc.head.id + ".json";

                        // пробуем записать файл блокировки, если файл есть, то он запишется с имененм вида {uuid}.tmp
                        try (FileInputStream fi = new FileInputStream(outfile)) {
                            if (!f.appendFile(lockFileName, fi)) {
                                throw new IOException("Ошибка при обмене с сервером! Не удалось отправить файл блокировки!");
                            }
                        } finally {
                            outfile.delete();
                        }

                        // читаем файл блокировки
                        File localcopy = File.createTempFile("lrfromoffice", "json", getCacheDir());
                        OutputStream output = new FileOutputStream(localcopy);

                        if (!f.retrieveFile(lockFileName, output)) {
                            output.close();
                            localcopy.delete();

                            throw new IOException("Ошибка при обмене с сервером! Не удалось получить файл блокировки!");
                        }
                        output.close();

                        try (BufferedReader br = new BufferedReader(new FileReader(localcopy))) {

                            String firstLine = br.readLine();
                            Type roundLockType = new TypeToken<RoundLock>() {}.getType();

                            RoundLock rlock = gson.fromJson(firstLine, roundLockType);

                            doc.head.driverId = rlock.driverId;
                            ((DeliveryApp) getApplication()).getDatabase().roundDocDAO().update(doc);

                            return rlock.driverId.equals(lock.driverId);

                        } finally {
                            localcopy.delete();
                        }
                    } finally {
                        if (f.isConnected()) {
                            f.disconnect();
                        }
                    }
                }
            }).subscribeOn(Schedulers.computation()).observeOn(AndroidSchedulers.mainThread()).
                    subscribeWith(new DisposableSingleObserver<Boolean>() {
                        @Override
                        public void onSuccess(Boolean aBoolean) {
                            if(aBoolean) {
                                Toast.makeText(FreeRoundDocActivity.this, R.string.takeFreeRound, Toast.LENGTH_LONG).show();
                            } else {
                                Toast.makeText(FreeRoundDocActivity.this, R.string.failTakeFreeRound, Toast.LENGTH_LONG).show();
                            }

                            finish();
                        }

                        @Override
                        public void onError(Throwable e) {
                            item.setEnabled(true);

                            Toast.makeText(FreeRoundDocActivity.this, "Проблемы со связью! Повторите попытку! " + e.getMessage(), Toast.LENGTH_LONG).show();
                            Log.e("freerounddoc", "ошибка", e);
                        }
                    }));
        }

        return super.onOptionsItemSelected(item);
    }
}
