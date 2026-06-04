package com.example.campusrun;

public class AvatarHelper {

    public static int getAvatarResId(int index) {
        switch (index) {
            case 1:
                return R.drawable.avatar_1;
            case 2:
                return R.drawable.avatar_2;
            case 3:
                return R.drawable.avatar_3;
            default:
                return R.drawable.avatar_0;
        }
    }
}
