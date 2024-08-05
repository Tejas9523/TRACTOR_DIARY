package com.example.tractordiary;


import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class adapter extends RecyclerView.Adapter<adapter.MyiewHolder> {

    ArrayList<User> list;

    public adapter(Context context, ArrayList<User> list) {
        this.list = list;
    }

    @NonNull
    @Override
    public MyiewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item,parent,false);
        return new MyiewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull MyiewHolder holder, @SuppressLint("RecyclerView") int position) {
        
        User user = list.get(position);
        holder.name.setText(user.getName());
        holder.date.setText(user.getDate());
        holder.stime.setText(user.getStime());
        holder.etime.setText(user.getEtime());
        holder.description.setText(user.getDescription());
        holder.amt.setText(user.getAmt());
        holder.amt2.setText(user.getAmt2());

        if (holder.amt2.getText().toString().equals("0")){
            holder.itemView.findViewById(R.id.item).setBackgroundResource(R.color.recieved);
        }
        else {
            holder.itemView.findViewById(R.id.item).setBackgroundResource(R.color.pending);
        }

        holder.itemView.findViewById(R.id.edit).setOnClickListener(view -> {
            PopupMenu popupMenu = new PopupMenu(view.getContext(),holder.itemView.findViewById(R.id.edit));
            popupMenu.getMenuInflater().inflate(R.menu.menu_item,popupMenu.getMenu());
            popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem menuItem) {
                    if (menuItem.getTitle().toString().equals("Edit")){
                        Dialog dialog = new Dialog(view.getContext());
                        dialog.setContentView(R.layout.update);

                        EditText nm1 = dialog.findViewById(R.id.name);
                        EditText dt1 = dialog.findViewById(R.id.date);
                        EditText tt1 = dialog.findViewById(R.id.time1);
                        EditText tt2 = dialog.findViewById(R.id.time2);
                        EditText ds = dialog.findViewById(R.id.des);
                        EditText amt1 = dialog.findViewById(R.id.amont);
                        EditText amt2 = dialog.findViewById(R.id.amont2);

                        Button update = dialog.findViewById(R.id.ud);
                        Button cle = dialog.findViewById(R.id.cancle);

                        nm1.setText(user.getName());
                        dt1.setText(user.getDate());
                        tt1.setText(user.getStime());
                        tt2.setText(user.getEtime());
                        ds.setText(user.getDescription());
                        amt1.setText(user.getAmt());
                        amt2.setText(user.getAmt2());

                        update.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {

                                Intent intent = new Intent(view.getContext(),home.class);
                                view.getContext().startActivity(intent);

                                Map<String,Object> map = new HashMap<>();
                                if (!amt1.getText().toString().equals("") && !amt2.getText().toString().equals("")){

                                    map.put("name",nm1.getText().toString());
                                    map.put("amt",amt1.getText().toString());
                                    map.put("amt2",amt2.getText().toString());
                                    map.put("date",dt1.getText().toString());
                                    map.put("stime",tt1.getText().toString());
                                    map.put("etime",tt2.getText().toString());
                                    map.put("description",ds.getText().toString());

                                    String id = FirebaseAuth.getInstance().getCurrentUser().getUid();
                                    FirebaseDatabase.getInstance().getReference("user").child(id).child(user.getKey()).updateChildren(map).addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {

                                            Toast.makeText(view.getContext(), "Successfully Updated ...", Toast.LENGTH_SHORT).show();

                                        }
                                    });

                                }
                                else{
                                    Toast.makeText(view.getContext(),"Invalid Amount !!!",Toast.LENGTH_SHORT).show();
                                }
                                dialog.dismiss();
                            }
                        });

                        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
                        layoutParams.copyFrom(dialog.getWindow().getAttributes());
                        layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT;
                        layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
                        dialog.getWindow().setAttributes(layoutParams);
                        dialog.show();

                        cle.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                dialog.dismiss();
                            }
                        });
                    }
                    if (menuItem.getTitle().toString().equals("Delete")){

                        AlertDialog.Builder dialog = new AlertDialog.Builder(view.getContext());
                        dialog.setMessage("Do you want to delete ?").setPositiveButton("Delete",((dialogInterface, i) -> {
                            String id = FirebaseAuth.getInstance().getCurrentUser().getUid();
                            FirebaseDatabase.getInstance().getReference("user").child(id).child(user.getKey()).removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    Intent intent = new Intent(view.getContext(),home.class);
                                    view.getContext().startActivity(intent);
                                    Toast.makeText(view.getContext(),"Deleted...",Toast.LENGTH_SHORT).show();
                                }
                            });
                        })).setNegativeButton("Cancle",(dialogInterface, i) -> {
                            dialogInterface.cancel();
                        });
                        dialog.show();
                        }
                    return true;
                }
            });
            popupMenu.show();

        });

    }

    @Override
    public int getItemCount() {
        return list.size();
    }


    public void filterList(ArrayList<User> filteredList){
        list = filteredList;
        notifyDataSetChanged();
    }

    public static class MyiewHolder extends RecyclerView.ViewHolder{

        TextView name,date,stime,etime,description,amt,amt2;

        public MyiewHolder(@NonNull View itemView) {
            super(itemView);

            name = itemView.findViewById(R.id.textview3);
            date = itemView.findViewById(R.id.textview5);
            stime = itemView.findViewById(R.id.textview7);
            etime = itemView.findViewById(R.id.textview9);
            description = itemView.findViewById(R.id.textview13);
            amt = itemView.findViewById(R.id.textview11);
            amt2 = itemView.findViewById(R.id.textview10);
        }
    }
}
