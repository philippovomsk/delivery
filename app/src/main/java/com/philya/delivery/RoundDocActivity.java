package com.philya.delivery;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;
import com.philya.delivery.db.Db;
import com.philya.delivery.db.RoundDoc;
import com.philya.delivery.db.RoundRow;
import io.reactivex.Completable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Action;
import io.reactivex.observers.DisposableCompletableObserver;
import io.reactivex.schedulers.Schedulers;

import java.util.Locale;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static com.philya.delivery.ExchangeJobService.docDateFormat;

public class RoundDocActivity extends AppCompatActivity implements View.OnClickListener {

    private RoundDoc doc;

    private CompositeDisposable rxDisposable;

    protected ConstraintLayout head;

    private TextView numberEdit;

    private TextView dateEdit;

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
        setTitle(R.string.roundDocTitle);

        rxDisposable = new CompositeDisposable();

        if (savedInstanceState != null && savedInstanceState.containsKey("doc")) {
            doc = (RoundDoc) savedInstanceState.getSerializable("doc");
        } else {
            doc = (RoundDoc) getIntent().getExtras().get("doc");
        }

        head = (ConstraintLayout) findViewById(R.id.head);
        numberEdit = (TextView) findViewById(R.id.numberEdit);
        dateEdit = (TextView) findViewById(R.id.dateEdit);
        carEdit = (TextView) findViewById(R.id.carEdit);
        fromEdit = (TextView) findViewById(R.id.fromEdit);
        completed = (CheckBox) findViewById(R.id.completed);
        weight = (TextView) findViewById(R.id.weight);
        contractPriceLabel = findViewById(R.id.contractPriceLabel);
        contractPrice = findViewById(R.id.contractPrice);

        head.setOnClickListener(this);
        completed.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (doc.head.complete != isChecked) {
                    doc.head.complete = isChecked;
                    saveDoc();
                }
            }
        });

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
        carEdit.setText(doc.head.car);
        fromEdit.setText(doc.head.from);
        completed.setChecked(doc.head.complete);
        weight.setText(String.format(Locale.getDefault(),"%d кг", doc.head.weight));
        contractPrice.setText(String.format(Locale.getDefault(),"%d-00", doc.head.contractPrice));

        if(doc.head.contractPrice == 0 && !doc.head.driverId.isEmpty()) {
            contractPriceLabel.setVisibility(GONE);
            contractPrice.setVisibility(GONE);
        } else {
            contractPriceLabel.setVisibility(VISIBLE);
            contractPrice.setVisibility(VISIBLE);
        }

        adapter.setRoundDoc(doc);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        rxDisposable.clear();
    }

    @Override
    public void onClick(View v) {
        doc.head.complete = !doc.head.complete;
        completed.setChecked(doc.head.complete);
        saveDoc();
    }

    public void saveDoc() {
        final Db database = ((DeliveryApp) getApplication()).getDatabase();

        rxDisposable.add(Completable.fromAction(new Action() {
            @Override
            public void run() throws Exception {
                database.roundDocDAO().update(doc);
            }
        }).subscribeOn(Schedulers.computation()).observeOn(AndroidSchedulers.mainThread()).
                subscribeWith(new DisposableCompletableObserver() {
                    @Override
                    public void onComplete() {
                        Log.d("delivery.roundDoc", "сохранен документ");

                        Exchange.startExchangeJob(getApplicationContext(), 10000);
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.e("delivery.roundDoc", "неудалось сохранить документ", e);
                    }
                }));
    }

}
