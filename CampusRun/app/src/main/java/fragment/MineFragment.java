package fragment;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.example.campusrun.InfoActivity;
import com.example.campusrun.MyOrderActivity;
import com.example.campusrun.AvatarHelper;
import com.example.campusrun.DBManager;
import com.example.campusrun.ProfileActivity;
import com.example.campusrun.R;
import com.example.campusrun.SettingsActivity;
import com.example.campusrun.WalletActivity;

public class MineFragment extends Fragment {

    private TextView tvUsername;
    private TextView tvMyPublish;
    private TextView tvMyAccept;
    private TextView tvWallet;
    private TextView tvService;
    private TextView tvSettings;
    private ImageView imgAvatar;
    private View layoutProfileHeader;
    private DBManager dbManager;
    private String username;

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_mine, container, false);

        tvUsername = view.findViewById(R.id.tvUsername);
        tvMyPublish = view.findViewById(R.id.tvMyPublish);
        tvMyAccept = view.findViewById(R.id.tvMyAccept);
        tvWallet = view.findViewById(R.id.tvWallet);
        tvService = view.findViewById(R.id.tvService);
        tvSettings = view.findViewById(R.id.tvSettings);
        imgAvatar = view.findViewById(R.id.imgAvatar);
        layoutProfileHeader = view.findViewById(R.id.layoutProfileHeader);
        dbManager = new DBManager(requireContext());

        SharedPreferences sp = requireActivity().getSharedPreferences("user", 0);
        username = sp.getString("username", "");
        refreshProfile();

        layoutProfileHeader.setOnClickListener(v ->
                startActivity(new Intent(getActivity(), ProfileActivity.class)));

        tvMyPublish.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), MyOrderActivity.class);
            intent.putExtra(MyOrderActivity.TYPE, MyOrderActivity.TYPE_PUBLISH);
            startActivity(intent);
        });

        tvMyAccept.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), MyOrderActivity.class);
            intent.putExtra(MyOrderActivity.TYPE, MyOrderActivity.TYPE_ACCEPT);
            startActivity(intent);
        });

        tvWallet.setOnClickListener(v ->
                startActivity(new Intent(getActivity(), WalletActivity.class)));

        tvService.setOnClickListener(v -> openInfo(
                "\u8054\u7cfb\u5ba2\u670d",
                "\u6821\u56ed\u8dd1\u817f\u5ba2\u670d\u4e2d\u5fc3\n\n\u670d\u52a1\u65f6\u95f4\uff1a9:00 - 22:00\n\u5ba2\u670d\u7535\u8bdd\uff1a400-123-2026\n\u5fae\u4fe1\uff1aCampusRunHelp\n\n\u6f14\u793a\u8bf4\u660e\uff1a\u5b9e\u9645\u4e0a\u7ebf\u540e\u53ef\u63a5\u5165\u5728\u7ebf\u5ba2\u670d\u6216\u610f\u89c1\u53cd\u9988\u8868\u3002"));

        tvSettings.setOnClickListener(v ->
                startActivity(new Intent(getActivity(), SettingsActivity.class)));

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (dbManager != null) {
            refreshProfile();
        }
    }

    private void refreshProfile() {
        if (username == null || username.isEmpty()) {
            tvUsername.setText("\u6821\u56ed\u8dd1\u817f");
            imgAvatar.setImageResource(AvatarHelper.getAvatarResId(0));
            return;
        }

        tvUsername.setText(dbManager.getNickname(username));
        String avatarUri = dbManager.getAvatarUri(username);
        if (avatarUri != null && !avatarUri.isEmpty()) {
            imgAvatar.setImageURI(Uri.parse(avatarUri));
        } else {
            imgAvatar.setImageResource(AvatarHelper.getAvatarResId(dbManager.getAvatarIndex(username)));
        }
    }

    private void openInfo(String title, String content) {
        Intent intent = new Intent(getActivity(), InfoActivity.class);
        intent.putExtra(InfoActivity.TITLE, title);
        intent.putExtra(InfoActivity.CONTENT, content);
        startActivity(intent);
    }
}
