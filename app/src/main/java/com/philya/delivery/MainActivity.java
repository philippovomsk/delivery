package com.philya.delivery;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
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
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Consumer;
import io.reactivex.observers.DisposableMaybeObserver;
import io.reactivex.schedulers.Schedulers;

import java.util.ArrayList;
import java.util.List;

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

        adapter = new RoundRowsAdapter();
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
    public boolean onPrepareOptionsMenu(Menu menu) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String lastexchange = preferences.getString("lastexchange", "");
        String title = getResources().getString(R.string.exchangeLabel);

        menu.getItem(0).setTitle(title + " / " + lastexchange);

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == R.id.menuSettings) {
            Intent settings = new Intent(this, SettingsActivity.class);
            startActivity(settings);
        } else if (item.getItemId() == R.id.menuExchange) {
            Exchange.startExchangeJob(getApplicationContext(), 0);
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

    public class RoundRowsAdapter extends RecyclerView.Adapter<RoundRowItemViewHolder> {

        private List<RoundDoc> docs = new ArrayList<>();

        @NonNull
        @Override
        public RoundRowItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.rounditem, parent, false);
            return new RoundRowItemViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull RoundRowItemViewHolder holder, int position) {
            holder.bind(docs.get(position), position);
        }

        @Override
        public int getItemCount() {
            return docs.size();
        }

        public void setDocs(List<RoundDoc> docs) {
            this.docs = docs;
            notifyDataSetChanged();
        }
    }

    public class RoundRowItemViewHolder extends RecyclerView.ViewHolder {

        protected ConstraintLayout view;

        private TextView numberEdit;

        private TextView dateEdit;

        private TextView carEdit;

        private TextView fromEdit;

        private CheckBox completed;

        private int firstColor;

        private int secondColor;

        public RoundRowItemViewHolder(View itemView) {
            super(itemView);

            view = (ConstraintLayout) itemView.findViewById(R.id.roundItem);
            numberEdit = (TextView) itemView.findViewById(R.id.numberEdit);
            dateEdit = (TextView) itemView.findViewById(R.id.dateEdit);
            carEdit = (TextView) itemView.findViewById(R.id.carEdit);
            fromEdit = (TextView) itemView.findViewById(R.id.fromEdit);
            completed = (CheckBox) itemView.findViewById(R.id.completed);

            TypedValue windowBackground = new TypedValue();
            getTheme().resolveAttribute(android.R.attr.windowBackground, windowBackground, true);
            if (windowBackground.type >= TypedValue.TYPE_FIRST_COLOR_INT && windowBackground.type <= TypedValue.TYPE_LAST_COLOR_INT) {
                firstColor = windowBackground.data;
            }

            secondColor = getResources().getColor(R.color.colorListRow);
        }

        public void bind(final RoundDoc doc, int position) {
            numberEdit.setText(doc.head.number);
            dateEdit.setText(docDateFormat.format(doc.head.date));
            carEdit.setText(doc.head.car);
            fromEdit.setText(doc.head.from);
            completed.setChecked(doc.head.complete);

            view.setBackgroundColor((position % 2 == 0) ? firstColor : secondColor);

            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent docintent = new Intent(MainActivity.this, RoundDocActivity.class);
                    docintent.putExtra("doc", doc);
                    startActivity(docintent);
                }
            });
        }
    }

}
