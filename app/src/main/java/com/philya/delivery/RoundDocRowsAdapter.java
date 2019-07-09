package com.philya.delivery;

import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;
import com.philya.delivery.db.RoundDoc;
import com.philya.delivery.db.RoundRow;

import java.util.ArrayList;
import java.util.List;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

public class RoundDocRowsAdapter extends RecyclerView.Adapter<RoundDocRowsAdapter.RoundDocRowItemViewHolder> {

    private AppCompatActivity activity;

    private RoundDoc doc;

    private List<RoundRow> rows = new ArrayList<>();

    public RoundDocRowsAdapter(AppCompatActivity activity) {
        this.activity = activity;
    }

    @NonNull
    @Override
    public RoundDocRowsAdapter.RoundDocRowItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.roundrowitem, parent, false);
        return new RoundDocRowsAdapter.RoundDocRowItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RoundDocRowsAdapter.RoundDocRowItemViewHolder holder, int position) {
        holder.bind(rows.get(position), position);
    }

    @Override
    public int getItemCount() {
        return rows.size();
    }

    public void setRoundDoc(RoundDoc doc) {
        this.doc = doc;
        this.rows = doc.rows;
        notifyDataSetChanged();
    }

    public class RoundDocRowItemViewHolder extends RecyclerView.ViewHolder {

        protected ConstraintLayout view;

        private TextView rowNumberEdit;

        private TextView pointEdit;

        private TextView addressEdit;

        private TextView phoneEdit;

        private TextView fioEdit;

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
            fioEdit = (TextView) itemView.findViewById(R.id.fioEdit);
            completed = (CheckBox) itemView.findViewById(R.id.completed);

            TypedValue windowBackground = new TypedValue();
            activity.getTheme().resolveAttribute(android.R.attr.windowBackground, windowBackground, true);
            if (windowBackground.type >= TypedValue.TYPE_FIRST_COLOR_INT && windowBackground.type <= TypedValue.TYPE_LAST_COLOR_INT) {
                firstColor = windowBackground.data;
            }

            secondColor = activity.getResources().getColor(R.color.colorListRow);
        }

        public void bind(final RoundRow row, int position) {
            rowNumberEdit.setText(Integer.toString(row.rowNumber));
            pointEdit.setText(row.point);
            addressEdit.setText(row.address);
            phoneEdit.setText(row.phone);
            fioEdit.setText(row.fio);
            completed.setChecked(row.complete);

            view.setBackgroundColor((position % 2 == 0) ? firstColor : secondColor);

            if(doc.head.driverId.isEmpty()) {
                completed.setVisibility(GONE);
            } else {
                completed.setVisibility(VISIBLE);

                view.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        row.complete = !row.complete;
                        completed.setChecked(row.complete);
                        ((RoundDocActivity) activity).saveDoc();
                    }
                });

                completed.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        if (row.complete != isChecked) {
                            row.complete = isChecked;
                            ((RoundDocActivity) activity).saveDoc();
                        }
                    }
                });
            }
        }

    }

}
