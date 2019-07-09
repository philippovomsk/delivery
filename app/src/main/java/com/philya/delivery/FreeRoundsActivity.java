package com.philya.delivery;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import com.philya.delivery.db.Db;
import com.philya.delivery.db.Driver;
import com.philya.delivery.db.RoundDoc;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Consumer;
import io.reactivex.observers.DisposableMaybeObserver;
import io.reactivex.schedulers.Schedulers;

import java.util.List;

public class FreeRoundsActivity extends AppCompatActivity {

    private CompositeDisposable rxSubscriptions;

    private RoundRowsAdapter adapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_freerounds);
        setTitle(R.string.freeRounds);

        rxSubscriptions = new CompositeDisposable();

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

        Db db = ((DeliveryApp) getApplication()).getDatabase();

        rxSubscriptions.add(db.roundDocDAO().listAllFree().
                subscribeOn(Schedulers.computation()).observeOn(AndroidSchedulers.mainThread()).
                subscribe(new Consumer<List<RoundDoc>>() {
                    @Override
                    public void accept(List<RoundDoc> roundDocs) throws Exception {
                        adapter.setDocs(roundDocs);
                    }
                }));
    }

}
