package com.philya.delivery;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import com.philya.delivery.db.Db;
import com.philya.delivery.db.Driver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

import java.util.ArrayList;
import java.util.List;

public class DriverActivity extends AppCompatActivity implements View.OnClickListener {

    private CompositeDisposable rxSubscriptions;

    private DriverRowsAdapter adapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver);

        setTitle(R.string.driverTitle);

        rxSubscriptions = new CompositeDisposable();

        TextView nonSelected = (TextView) findViewById(R.id.nonSelected);
        nonSelected.setOnClickListener(this);

        RecyclerView driversList = (RecyclerView) findViewById(R.id.drivers);

        adapter = new DriverRowsAdapter();
        driversList.setAdapter(adapter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        rxSubscriptions.clear();
    }


    @Override
    protected void onResume() {
        super.onResume();

        Db database = ((DeliveryApp) getApplication()).getDatabase();

        rxSubscriptions.add(database.driverDAO().listAll().
                subscribeOn(Schedulers.computation()).observeOn(AndroidSchedulers.mainThread()).
                subscribe(new Consumer<List<Driver>>() {
                    @Override
                    public void accept(List<Driver> drivers) throws Exception {
                        adapter.setDrivers(drivers);
                    }
                }));
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.nonSelected) {
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            preferences.edit().remove("currentDriverId").apply();
            finish();
        }
    }

    public class DriverRowsAdapter extends RecyclerView.Adapter<DriverRowItemViewHolder> {

        private List<Driver> drivers = new ArrayList<>();

        @NonNull
        @Override
        public DriverRowItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.driveritem, parent, false);
            return new DriverRowItemViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull DriverRowItemViewHolder holder, int position) {
            holder.bind(drivers.get(position), position);
        }

        @Override
        public int getItemCount() {
            return drivers.size();
        }

        public void setDrivers(List<Driver> drivers) {
            this.drivers = drivers;
            notifyDataSetChanged();
        }
    }

    public class DriverRowItemViewHolder extends RecyclerView.ViewHolder {

        private TextView name;

        private int firstColor;

        private int secondColor;

        public DriverRowItemViewHolder(View itemView) {
            super(itemView);

            name = (TextView) itemView.findViewById(R.id.name);

            TypedValue windowBackground = new TypedValue();
            getTheme().resolveAttribute(android.R.attr.windowBackground, windowBackground, true);
            if (windowBackground.type >= TypedValue.TYPE_FIRST_COLOR_INT && windowBackground.type <= TypedValue.TYPE_LAST_COLOR_INT) {
                firstColor = windowBackground.data;
            }

            secondColor = getResources().getColor(R.color.colorListRow);
        }

        public void bind(final Driver driver, int position) {
            name.setText(driver.name);
            name.setBackgroundColor((position % 2 == 1) ? firstColor : secondColor);

            name.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(DriverActivity.this);
                    builder.setTitle(R.string.passwordTitle);

                    View dialogView = getLayoutInflater().inflate(R.layout.passworddialog, null);
                    final TextView passwordInput = (TextView) dialogView.findViewById(R.id.passwordInput);

                    builder.setView(dialogView);
                    builder.setNegativeButton(R.string.cancelLabel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    });

                    builder.setPositiveButton(R.string.okLabel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if(driver.password.equals(passwordInput.getText().toString())) {
                                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                                preferences.edit().putString("currentDriverId", driver.id).apply();
                                finish();
                            } else {
                                Toast.makeText(DriverActivity.this, R.string.badPassword, Toast.LENGTH_SHORT).show();
                            }
                        }
                    });

                    Dialog passwordDialog = builder.create();
                    passwordDialog.show();
                }
            });
        }
    }

}
