package com.example.printingsample;

import android.bluetooth.BluetoothDevice;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class BTName extends RecyclerView.Adapter<BTName.MyViewHolder> {
    ArrayList<BluetoothDevice> names;
    Senddata senddata;
    public BTName(ArrayList<BluetoothDevice> names, Senddata senddata) {
        this.names=names;
        this.senddata=senddata;
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
       View view= LayoutInflater.from(parent.getContext()).inflate(R.layout.item_list,parent,false);
        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {
    holder.tv_name.setText(names.get(position).getName());
        holder.tv_name.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                senddata.sendPos(holder.getAdapterPosition());
            }
        });
    }

    @Override
    public int getItemCount() {
        return names.size();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {
        TextView tv_name;
        public MyViewHolder(View itemView) {
            super(itemView);
            tv_name=itemView.findViewById(R.id.tv_name);
        }
    }


    public interface Senddata{
        public  void sendPos(int i);
    }


}
