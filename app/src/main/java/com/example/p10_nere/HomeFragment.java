package com.example.p10_nere;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;


public class HomeFragment extends Fragment {

    NavController navController;

    public AppViewModel appViewModel;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        navController = Navigation.findNavController(view);

        appViewModel = new
                ViewModelProvider(requireActivity()).get(AppViewModel.class);

        view.findViewById(R.id.gotoNewPostFragmentButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navController.navigate(R.id.newPostFragment);
            }
        });

        RecyclerView postsRecyclerView = view.findViewById(R.id.postsRecyclerView);

        Query query = getQuery();

        FirestoreRecyclerOptions<Post> options = new FirestoreRecyclerOptions.Builder<Post>()
                .setQuery(query, Post.class)
                .setLifecycleOwner(this)
                .build();

        postsRecyclerView.setAdapter(new PostsAdapter(options));
    }

    Query getQuery() {
        return FirebaseFirestore.getInstance().collection("posts").orderBy("timeStamp", Query.Direction.DESCENDING).limit(50);
    }

    class PostsAdapter extends FirestoreRecyclerAdapter<Post, PostsAdapter.PostViewHolder> {

        public PostsAdapter(@NonNull FirestoreRecyclerOptions<Post> options) {
            super(options);
        }

        @NonNull
        @Override
        public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new PostViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.viewholder_post, parent, false));
        }

        @Override
        protected void onBindViewHolder(@NonNull PostViewHolder holder, int position, @NonNull final Post post) {
            Glide.with(getContext()).load(post.authorPhotoUrl).circleCrop().into(holder.authorPhotoImageView);
            holder.authorTextView.setText(post.author);
            holder.contentTextView.setText(post.content);

            String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

            //botón likes
            if (post.likes.containsKey(uid)) {
                holder.likeImageView.setImageResource(R.drawable.like_on);
            } else {
                holder.likeImageView.setImageResource(R.drawable.like_off);
            }
            holder.numLikesTextView.setText(String.valueOf(post.likes.size()));
            holder.likeImageView.setOnClickListener(view -> {
                FirebaseFirestore.getInstance().collection("posts")
                        .document(getSnapshots().getSnapshot(position).getId())
                        .update("likes." + uid, post.likes.containsKey(uid) ?
                                FieldValue.delete() : true);
            });

            // botón favoritos
            holder.favoritesImageView.setOnClickListener(view -> {
                String postId = getSnapshots().getSnapshot(position).getId();
                FirebaseFirestore db = FirebaseFirestore.getInstance();
                CollectionReference favoritesCollection = db.collection("favorites_" + uid);

                // Agregar el post a la colección de favoritos directamente
                addPostToFavorites(post, favoritesCollection, holder, uid);
            });

            SharedPreferences sharedPreferences = holder.itemView.getContext().getSharedPreferences("Favorites", Context.MODE_PRIVATE);
            String postId = getSnapshots().getSnapshot(position).getId();
            boolean isFavorite = sharedPreferences.getBoolean(postId, false);

            if (isFavorite) {
                holder.favoritesImageView.setImageResource(R.drawable.star_post2);
            } else {
                holder.favoritesImageView.setImageResource(R.drawable.star_post);
            }

            //para borrar el post
            String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            if (post.uid.equals(currentUserId)) {
                holder.deleteImageView.setVisibility(View.VISIBLE);
                holder.deleteImageView.setOnClickListener(view -> {
                    CollectionReference favoritesCollection = FirebaseFirestore.getInstance().collection("favorites_" + currentUserId);
                    removePost(postId, favoritesCollection, holder);
                });
            } else {
                holder.deleteImageView.setVisibility(View.GONE);
            }


            //miniaturas multimedia
            if (post.mediaUrl != null) {
                holder.mediaImageView.setVisibility(View.VISIBLE);
                if ("audio".equals(post.mediaType)) {
                    Glide.with(requireView()).load(R.drawable.audio).centerCrop().into(holder.mediaImageView);
                } else {
                    Glide.with(requireView()).load(post.mediaUrl).centerCrop().into(holder.mediaImageView);
                }
                holder.mediaImageView.setOnClickListener(view -> {
                    appViewModel.postSeleccionado.setValue(post);
                    navController.navigate(R.id.mediaFragment);
                });
            } else {
                holder.mediaImageView.setVisibility(View.GONE);
            }

            //fecha y hora de los post
            SimpleDateFormat format = new SimpleDateFormat("HH:mm dd/MM/yyyy");
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(post.timeStamp);
            holder.timeTextView.setText(format.format(calendar.getTime()));
        }


        class PostViewHolder extends RecyclerView.ViewHolder {
            ImageView authorPhotoImageView, likeImageView, mediaImageView, favoritesImageView, deleteImageView;
            TextView authorTextView, contentTextView, numLikesTextView, timeTextView;

            PostViewHolder(@NonNull View itemView) {
                super(itemView);
                authorPhotoImageView = itemView.findViewById(R.id.photoImageView);
                likeImageView = itemView.findViewById(R.id.likeImageView);
                mediaImageView = itemView.findViewById(R.id.mediaImage);
                authorTextView = itemView.findViewById(R.id.authorTextView);
                contentTextView = itemView.findViewById(R.id.contentTextView);
                numLikesTextView = itemView.findViewById(R.id.numLikesTextView);
                timeTextView = itemView.findViewById(R.id.timeTextView);
                favoritesImageView = itemView.findViewById(R.id.favoritesImageView);
                deleteImageView = itemView.findViewById(R.id.deleteImageView);
            }
        }

        private void addPostToFavorites(Post post, CollectionReference favoritesCollection, PostViewHolder holder, String uid) {
            String postId = getSnapshots().getSnapshot(holder.getAdapterPosition()).getId();
            DocumentReference postRef = favoritesCollection.document(postId);

            postRef.get().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        //si el post ya está en favoritos, eliminarlo
                        favoritesCollection.document(postId)
                                .delete()
                                .addOnSuccessListener(aVoid -> {
                                    //cambiar el icono
                                    holder.favoritesImageView.setImageResource(R.drawable.star_post);
                                    notifyDataSetChanged();
                                    //actualizar share preferences
                                    SharedPreferences sharedPreferences = holder.itemView.getContext().getSharedPreferences("Favorites", Context.MODE_PRIVATE);
                                    SharedPreferences.Editor editor = sharedPreferences.edit();
                                    //eliminar post de favoritos
                                    editor.putBoolean(postId, false);
                                    editor.apply();
                                })
                                .addOnFailureListener(e -> {
                                });
                    } else {
                        //agregar el post a favoritos
                        postRef.set(post)
                                .addOnSuccessListener(aVoid -> {
                                    holder.favoritesImageView.setImageResource(R.drawable.star_post2);
                                    post.favorites.put(uid, true);
                                    updatePostInPostsCollection(postId, post);
                                    notifyDataSetChanged();
                                    SharedPreferences sharedPreferences = holder.itemView.getContext().getSharedPreferences("Favorites", Context.MODE_PRIVATE);
                                    SharedPreferences.Editor editor = sharedPreferences.edit();
                                    editor.putBoolean(postId, true);
                                    editor.apply();
                                })
                                .addOnFailureListener(e -> {

                                });
                    }
                } else {
                }
            });
        }

        private void updatePostInPostsCollection(String postId, Post post) {
            FirebaseFirestore.getInstance().collection("posts")
                    .document(postId)
                    .set(post)
                    .addOnSuccessListener(aVoid -> {
                    })
                    .addOnFailureListener(e -> {
                    });
        }


        //para poder eliminar el post
        private void removePost(String postId, CollectionReference favoritesCollection, PostViewHolder holder) {
            //primero cambiamos el sharedpreferences
            SharedPreferences sharedPreferences = holder.itemView.getContext().getSharedPreferences("Favorites", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.remove(postId);
            editor.apply();

            //luego eliminamos el post tanto en la colección posts como en favoritos
            FirebaseFirestore.getInstance().collection("posts").document(postId).delete()
                    .addOnSuccessListener(aVoid -> {

                        favoritesCollection.document(postId).delete()
                                .addOnSuccessListener(aVoid1 -> {
                                    notifyDataSetChanged();
                                })
                                .addOnFailureListener(e -> {
                                });
                    })
                    .addOnFailureListener(e -> {
                    });
        }

    }


}