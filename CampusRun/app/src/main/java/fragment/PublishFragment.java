package fragment;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.example.campusrun.DBManager;
import com.example.campusrun.R;

import bean.Task;

public class PublishFragment extends Fragment {

    EditText etTitle, etPickup, etDelivery, etPhone, etPrice, etRemark;
    Button btnPublish;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_publish, container, false);

        etTitle = view.findViewById(R.id.et_title);
        etPickup = view.findViewById(R.id.et_pickup);
        etDelivery = view.findViewById(R.id.et_delivery);
        etPhone = view.findViewById(R.id.et_phone);
        etPrice = view.findViewById(R.id.et_price);
        etRemark = view.findViewById(R.id.et_remark);
        btnPublish = view.findViewById(R.id.btn_publish);

        btnPublish.setOnClickListener(v -> publishTask());
        return view;
    }

    private void publishTask() {
        String title = etTitle.getText().toString().trim();
        String pickup = etPickup.getText().toString().trim();
        String delivery = etDelivery.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String priceText = etPrice.getText().toString().trim();
        String remark = etRemark.getText().toString().trim();

        if (TextUtils.isEmpty(title) || TextUtils.isEmpty(pickup)
                || TextUtils.isEmpty(delivery) || TextUtils.isEmpty(phone)
                || TextUtils.isEmpty(priceText)) {
            Toast.makeText(getContext(), "\u8bf7\u586b\u5199\u5b8c\u6574\u4fe1\u606f", Toast.LENGTH_SHORT).show();
            return;
        }

        double price;
        try {
            price = Double.parseDouble(priceText);
        } catch (NumberFormatException e) {
            Toast.makeText(getContext(), "\u4f63\u91d1\u683c\u5f0f\u4e0d\u6b63\u786e", Toast.LENGTH_SHORT).show();
            return;
        }

        SharedPreferences sp = requireActivity().getSharedPreferences("user", 0);
        String publisher = sp.getString("username", "\u672a\u77e5\u7528\u6237");
        Task task = new Task(title, pickup, delivery, phone, remark, price, publisher);
        DBManager dbManager = new DBManager(getContext());

        if (dbManager.publishTask(task)) {
            Toast.makeText(getContext(), "\u53d1\u5e03\u6210\u529f\uff0c\u4f63\u91d1\u5df2\u51bb\u7ed3", Toast.LENGTH_SHORT).show();
            etTitle.setText("");
            etPickup.setText("");
            etDelivery.setText("");
            etPhone.setText("");
            etPrice.setText("");
            etRemark.setText("");
        } else {
            Toast.makeText(getContext(), "\u53d1\u5e03\u5931\u8d25\uff0c\u8bf7\u68c0\u67e5\u4f59\u989d", Toast.LENGTH_SHORT).show();
        }
    }
}
