package com.philya.delivery;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.util.TypedValue;
import android.view.*;
import android.widget.CheckBox;
import android.widget.TextView;
import com.philya.delivery.db.Db;
import com.philya.delivery.db.Driver;
import com.philya.delivery.db.RoundDoc;
import io.reactivex.Completable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Consumer;
import io.reactivex.observers.DisposableMaybeObserver;
import io.reactivex.observers.DisposableSingleObserver;
import io.reactivex.schedulers.Schedulers;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static com.philya.delivery.ExchangeJobService.docDateFormat;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private CompositeDisposable rxSubscriptions;

    private TextView driverEdit;

    private RoundRowsAdapter adapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        rxSubscriptions = new CompositeDisposable();

        driverEdit = (TextView) findViewById(R.id.driverEdit);
        driverEdit.setOnClickListener(this);

        RecyclerView rounds = (RecyclerView) findViewById(R.id.rounds);

        adapter = new RoundRowsAdapter(this);
        rounds.setAdapter(adapter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        rxSubscriptions.clear();
    }

    @Override
    protected void onResume() {
        super.onResume();

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String currentDriverId = preferences.getString("currentDriverId", "drivernotselect");

        Db db = ((DeliveryApp) getApplication()).getDatabase();

        rxSubscriptions.add(db.driverDAO().getById(currentDriverId).
                subscribeOn(Schedulers.computation()).observeOn(AndroidSchedulers.mainThread()).
                subscribeWith(new DisposableMaybeObserver<Driver>() {
                    @Override
                    public void onSuccess(Driver driver) {
                        driverEdit.setText(driver.name);
                    }

                    @Override
                    public void onError(Throwable e) {
                        driverEdit.setText(R.string.driverHint);
                    }

                    @Override
                    public void onComplete() {
                        driverEdit.setText(R.string.driverHint);
                    }
                }));

        rxSubscriptions.add(db.roundDocDAO().listAll(currentDriverId).
                subscribeOn(Schedulers.computation()).observeOn(AndroidSchedulers.mainThread()).
                subscribe(new Consumer<List<RoundDoc>>() {
                    @Override
                    public void accept(List<RoundDoc> roundDocs) throws Exception {
                        adapter.setDocs(roundDocs);
                    }
                }));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.mainactivitymenu, menu);

        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(final Menu menu) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String lastexchange = preferences.getString("lastexchange", "");
        String title = getResources().getString(R.string.exchangeLabel);

        menu.getItem(1).setTitle(title + " / " + lastexchange);

        String currentDriverId = preferences.getString("currentDriverId", "drivernotselect");
        if(currentDriverId.equals("drivernotselect")) {
            menu.getItem(0).setEnabled(false);
        } else {
            menu.getItem(0).setEnabled(true);
            rxSubscriptions.add(((DeliveryApp) getApplication()).getDatabase().roundDocDAO().countFreeRounds().
                    subscribeOn(Schedulers.computation()).observeOn(AndroidSchedulers.mainThread()).
                    subscribe(new Consumer<Integer>() {
                        @Override
                        public void accept(Integer val) throws Exception {
                            String freeRounds = getResources().getString(R.string.freeRounds);
                            menu.getItem(0).setTitle(freeRounds + " (" + val + ")");
                        }
                    }));
        }

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == R.id.menuSettings) {
            Intent settings = new Intent(this, SettingsActivity.class);
            startActivity(settings);
        } else if (item.getItemId() == R.id.menuExchange) {
            Exchange.startExchangeJob(getApplicationContext(), 0);
        } else if (item.getItemId() == R.id.freeRounds) {
            Intent settings = new Intent(this, FreeRoundsActivity.class);
            startActivity(settings);
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.driverEdit) {
            Intent startActivity = new Intent(this, DriverActivity.class);
            startActivity(startActivity);
        }
    }


}
