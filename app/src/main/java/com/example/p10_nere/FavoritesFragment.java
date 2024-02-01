package com.example.p10_nere;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

public class FavoritesFragment extends HomeFragment {

    Query query = getQuery();

    Query getQuery(){
        return FirebaseFirestore.getInstance().collection("favorites").orderBy("timeStamp", Query.Direction.DESCENDING).limit(50);
    }
}