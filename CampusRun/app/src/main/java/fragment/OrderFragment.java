package fragment;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.campusrun.DBManager;
import com.example.campusrun.DetailActivity;
import com.example.campusrun.R;

import java.util.List;

import adapter.TaskAdapter;
import bean.Task;

public class OrderFragment extends Fragment {

    private RecyclerView recyclerView;
    private TextView tvEmpty;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_order, container, false);
        recyclerView = view.findViewById(R.id.recycler_order);
        tvEmpty = view.findViewById(R.id.tv_order_empty);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        loadData();
        return view;
    }

    private void loadData() {
        SharedPreferences sp = requireActivity().getSharedPreferences("user", 0);
        String username = sp.getString("username", "");
        DBManager dbManager = new DBManager(getContext());
        List<Task> tasks = dbManager.getMyAccept(username);

        tvEmpty.setVisibility(tasks.isEmpty() ? View.VISIBLE : View.GONE);
        recyclerView.setAdapter(new TaskAdapter(tasks, task -> {
            Intent intent = new Intent(getContext(), DetailActivity.class);
            intent.putExtra("taskId", task.getId());
            startActivity(intent);
        }));
    }

    @Override
    public void onResume() {
        super.onResume();
        if (recyclerView != null) {
            loadData();
        }
    }
}
