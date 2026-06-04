package adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.campusrun.DBManager;
import com.example.campusrun.R;

import java.util.List;

import bean.WalletRecord;

public class WalletRecordAdapter extends RecyclerView.Adapter<WalletRecordAdapter.ViewHolder> {

    private final List<WalletRecord> records;

    public WalletRecordAdapter(List<WalletRecord> records) {
        this.records = records;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_wallet_record, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        WalletRecord record = records.get(position);
        holder.tvDesc.setText(record.getDescription());
        String prefix = record.getAmount() >= 0 ? "+" : "";
        holder.tvAmount.setText(prefix + DBManager.money(record.getAmount()));
        holder.tvTime.setText(record.getCreateTime());
    }

    @Override
    public int getItemCount() {
        return records.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDesc, tvAmount, tvTime;

        ViewHolder(View view) {
            super(view);
            tvDesc = view.findViewById(R.id.tvRecordDesc);
            tvAmount = view.findViewById(R.id.tvRecordAmount);
            tvTime = view.findViewById(R.id.tvRecordTime);
        }
    }
}
