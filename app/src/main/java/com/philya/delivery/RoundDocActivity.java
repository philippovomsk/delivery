package com.philya.delivery;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;
import com.philya.delivery.db.Db;
import com.philya.delivery.db.RoundDoc;
import com.philya.delivery.db.RoundRow;
import io.reactivex.Completable;
import io.reactivex.CompletableObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Action;
import io.reactivex.observers.DisposableCompletableObserver;
import io.reactivex.schedulers.Schedulers;

import java.util.ArrayList;
import java.util.List;

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

        adapter = new RoundDocRowsAdapter();
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

        adapter.setRows(doc.rows);
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

    private void saveDoc() {
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

    public class RoundDocRowsAdapter extends RecyclerView.Adapter<RoundDocRowItemViewHolder> {

        private List<RoundRow> rows = new ArrayList<>();

        @NonNull
        @Override
        public RoundDocRowItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.roundrowitem, parent, false);
            return new RoundDocRowItemViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull RoundDocRowItemViewHolder holder, int position) {
            holder.bind(rows.get(position), position);
        }

        @Override
        public int getItemCount() {
            return rows.size();
        }

        public void setRows(List<RoundRow> rows) {
            this.rows = rows;
            notifyDataSetChanged();
        }
    }

    public class RoundDocRowItemViewHolder extends RecyclerView.ViewHolder {

        protected ConstraintLayout view;

        private TextView rowNumberEdit;

        private TextView pointEdit;

        private TextView addressEdit;

        private TextView phoneEdit;

        private CheckBox completed;

        private int firstColor;

        private int secondColor;

        public RoundDocRowItemViewHolder(View itemView) {
            super(itemView);

            view = (ConstraintLayout) itemView.findViewById(R.id.roundrowitem);
            rowNumberEdit = (TextView) itemView.findViewById(R.id.rowNumberEdit);
            pointEdit = (TextView) itemView.findViewById(R.id.pointEdit);
            addressEdit = (TextView) itemView.findViewById(R.id.addressEdit);
            phoneEdit = (TextView) itemView.findViewById(R.id.phoneEdit);
            completed = (CheckBox) itemView.findViewById(R.id.completed);

            TypedValue windowBackground = new TypedValue();
            getTheme().resolveAttribute(android.R.attr.windowBackground, windowBackground, true);
            if (windowBackground.type >= TypedValue.TYPE_FIRST_COLOR_INT && windowBackground.type <= TypedValue.TYPE_LAST_COLOR_INT) {
                firstColor = windowBackground.data;
            }

            secondColor = getResources().getColor(R.color.colorListRow);
        }

        public void bind(final RoundRow row, int position) {
            rowNumberEdit.setText(Integer.toString(row.rowNumber));
            pointEdit.setText(row.point);
            addressEdit.setText(row.address);
            phoneEdit.setText(row.phone);
            completed.setChecked(row.complete);

            view.setBackgroundColor((position % 2 == 0) ? firstColor : secondColor);

            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    row.complete = !row.complete;
                    completed.setChecked(row.complete);
                    saveDoc();

                }
            });

            completed.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (row.complete != isChecked) {
                        row.complete = isChecked;
                        saveDoc();
                    }
                }
            });
        }

    }

}
