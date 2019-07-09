package com.philya.delivery;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;
import com.philya.delivery.db.RoundDoc;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static com.philya.delivery.ExchangeJobService.docDateFormat;

public class RoundRowsAdapter extends RecyclerView.Adapter<RoundRowsAdapter.RoundRowItemViewHolder> {

    private List<RoundDoc> docs = new ArrayList<>();

    private AppCompatActivity activity;

    public RoundRowsAdapter(AppCompatActivity activity) {
        this.activity = activity;
    }

    @NonNull
    @Override
    public RoundRowsAdapter.RoundRowItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.rounditem, parent, false);
        return new RoundRowsAdapter.RoundRowItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RoundRowsAdapter.RoundRowItemViewHolder holder, int position) {
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

    public class RoundRowItemViewHolder extends RecyclerView.ViewHolder {

        protected ConstraintLayout view;

        private TextView numberEdit;

        private TextView dateEdit;

        private TextView carLabel;

        private TextView carEdit;

        private TextView fromEdit;

        private CheckBox completed;

        private TextView weight;

        private TextView contractPriceLabel;

        private TextView contractPrice;

        private int firstColor;

        private int secondColor;

        public RoundRowItemViewHolder(View itemView) {
            super(itemView);

            view = (ConstraintLayout) itemView.findViewById(R.id.roundItem);
            numberEdit = (TextView) itemView.findViewById(R.id.numberEdit);
            dateEdit = (TextView) itemView.findViewById(R.id.dateEdit);
            carLabel = itemView.findViewById(R.id.carLabel);
            carEdit = (TextView) itemView.findViewById(R.id.carEdit);
            fromEdit = (TextView) itemView.findViewById(R.id.fromEdit);
            completed = (CheckBox) itemView.findViewById(R.id.completed);
            weight = (TextView) itemView.findViewById(R.id.weight);
            contractPriceLabel = itemView.findViewById(R.id.contractPriceLabel);
            contractPrice = itemView.findViewById(R.id.contractPrice);

            TypedValue windowBackground = new TypedValue();
            activity.getTheme().resolveAttribute(android.R.attr.windowBackground, windowBackground, true);
            if (windowBackground.type >= TypedValue.TYPE_FIRST_COLOR_INT && windowBackground.type <= TypedValue.TYPE_LAST_COLOR_INT) {
                firstColor = windowBackground.data;
            }

            secondColor = activity.getResources().getColor(R.color.colorListRow);
        }

        public void bind(final RoundDoc doc, int position) {
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

            if(doc.head.driverId.isEmpty()) {
                completed.setVisibility(GONE);
                carLabel.setVisibility(GONE);
                carEdit.setVisibility(GONE);
            } else {
                completed.setVisibility(VISIBLE);
                carLabel.setVisibility(VISIBLE);
                carEdit.setVisibility(VISIBLE);
            }

            view.setBackgroundColor((position % 2 == 0) ? firstColor : secondColor);

            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent docintent = new Intent(activity, (doc.head.driverId.isEmpty() ? FreeRoundDocActivity.class : RoundDocActivity.class));
                    docintent.putExtra("doc", doc);
                    activity.startActivity(docintent);
                }
            });
        }
    }

}
