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
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;

public class RouteListView extends AppCompatActivity {

    ListView listView;
    TextView empty;
    private DatabaseReference Route;
    private DatabaseReference SRoute;
    private String BusSelection;
    String SID = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();
    ArrayAdapter<Object> adapter;
    ArrayList<Object> list_array = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_route_list_view);

        listView = (ListView) findViewById(R.id.Listview);

        empty = (TextView) findViewById(R.id.EmptyText);

        Route = FirebaseDatabase.getInstance().getReference().child("Routes");

        SRoute = FirebaseDatabase.getInstance().getReference().child("Users").child("Students").child(SID).child("Selection");

        listView.setEmptyView(empty);

        Initialization();

        Retrieve();

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                String RouteSelected = adapterView.getItemAtPosition(position).toString();

                Intent intent = new Intent(RouteListView.this, ClientMap.class);

                HashMap Route = new HashMap();
                Route.put("Route", RouteSelected);
                SRoute.updateChildren(Route);


                startActivity(intent);
            }
        });
    }

    private void Initialization() {
        adapter = new ArrayAdapter<Object>(RouteListView.this, android.R.layout.simple_list_item_1, list_array);
        listView.setAdapter(adapter);
    }

    private void Retrieve() {
        Route.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Set<Object> set = new HashSet<>();
                Iterator iterator = dataSnapshot.getChildren().iterator();

                while (iterator.hasNext()) {
                    set.add(((DataSnapshot) iterator.next()).getValue());
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
