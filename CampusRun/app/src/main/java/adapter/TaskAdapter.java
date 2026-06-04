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

import bean.Task;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.ViewHolder> {

    private final List<Task> taskList;
    private final OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(Task task);
    }

    public TaskAdapter(List<Task> taskList, OnItemClickListener listener) {
        this.taskList = taskList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_task, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Task task = taskList.get(position);
        holder.tvTitle.setText(task.getTitle());
        holder.tvRoute.setText(task.getRoute());
        holder.tvPrice.setText("\u00a5" + DBManager.money(task.getPrice()));
        holder.tvStatus.setText(task.getState());

        if (DBManager.STATE_WAITING.equals(task.getState())) {
            holder.tvStatus.setTextColor(0xFF2E7D32);
        } else if (DBManager.STATE_ACCEPTED.equals(task.getState())) {
            holder.tvStatus.setTextColor(0xFF1976D2);
        } else {
            holder.tvStatus.setTextColor(0xFF757575);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(task);
        });
    }

    @Override
    public int getItemCount() {
        return taskList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvRoute, tvPrice, tvStatus;

        ViewHolder(View view) {
            super(view);
            tvTitle = view.findViewById(R.id.tvTitle);
            tvRoute = view.findViewById(R.id.tvRoute);
            tvPrice = view.findViewById(R.id.tvPrice);
            tvStatus = view.findViewById(R.id.tvStatus);
        }
    }
}
