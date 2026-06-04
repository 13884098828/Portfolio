package fragment;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.campusrun.DBManager;
import com.example.campusrun.DataCenter;
import com.example.campusrun.DetailActivity;
import com.example.campusrun.R;

import adapter.TaskAdapter;

public class HomeFragment extends Fragment {

    RecyclerView recyclerView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        loadData();
        return view;
    }

    private void loadData() {
        SharedPreferences sp = requireActivity().getSharedPreferences("user", 0);
        String currentUser = sp.getString("username", "");
        DBManager dbManager = new DBManager(getContext());
        DataCenter.taskList.clear();
        DataCenter.taskList.addAll(dbManager.getWaitingTasks(currentUser));

        TaskAdapter adapter = new TaskAdapter(DataCenter.taskList, task -> {
            Intent intent = new Intent(getContext(), DetailActivity.class);
            intent.putExtra("taskId", task.getId());
            startActivity(intent);
        });

        recyclerView.setAdapter(adapter);
    }

    @Override
    public void onResume() {
        super.onResume();
        loadData();
    }
}
