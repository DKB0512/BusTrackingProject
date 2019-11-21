package com.example.studentbustracker;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;

public class BusListView extends AppCompatActivity {

    ListView listView;
    TextView empty;
    private DatabaseReference Bus, SBus;
    String SID = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();
    ArrayAdapter<Object> adapter;
    ArrayList<Object> list_array = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bus_list_view);

        listView = (ListView) findViewById(R.id.Listview);

        empty = (TextView) findViewById(R.id.EmptyText);

        Bus = FirebaseDatabase.getInstance().getReference().child("Buses");

        SBus = FirebaseDatabase.getInstance().getReference().child("Users").child("Students").child(SID).child("Selection");

        Initialization();

        Retrieve();

        listView.setEmptyView(empty);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                String BusSelected = adapterView.getItemAtPosition(position).toString();

                Intent intent = new Intent(BusListView.this, ClientMap.class);

                HashMap Bus = new HashMap();
                Bus.put("Bus", BusSelected);
                SBus.updateChildren(Bus);

                startActivity(intent);
            }
        });
    }

    private void Initialization() {
        adapter = new ArrayAdapter<Object>(BusListView.this, android.R.layout.simple_list_item_1, list_array);
        listView.setAdapter(adapter);
    }

    private void Retrieve() {
        Bus.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Set<Object> set = new HashSet<>();
                Iterator iterator = dataSnapshot.getChildren().iterator();

                while (iterator.hasNext()) {
                    set.add((((DataSnapshot) iterator.next()).getValue()));
                }
                list_array.clear();
                list_array.addAll(set);
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }
}
