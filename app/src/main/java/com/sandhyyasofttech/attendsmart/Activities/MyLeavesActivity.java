package com.sandhyyasofttech.attendsmart.Activities;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.sandhyyasofttech.attendsmart.Adapters.MyLeavesAdapter;
import com.sandhyyasofttech.attendsmart.Models.LeaveModel;
import com.sandhyyasofttech.attendsmart.R;
import com.sandhyyasofttech.attendsmart.Utils.PrefManager;

import java.util.ArrayList;

public class MyLeavesActivity extends AppCompatActivity {

    private RecyclerView rv;
    private ArrayList<LeaveModel> list = new ArrayList<>();
    private MyLeavesAdapter adapter;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_my_leaves);

        rv = findViewById(R.id.rvLeaves);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MyLeavesAdapter(list);
        rv.setAdapter(adapter);

        loadLeaves();
        // loadLeaves() call नंतर add करा:
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

    }

    private void loadLeaves() {
        PrefManager pref = new PrefManager(this);
        String companyKey = pref.getCompanyKey();
        String mobile = pref.getEmployeeMobile();

        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("Companies")
                .child(companyKey)
                .child("leaves");

        Query q = ref.orderByChild("employeeMobile").equalTo(mobile);

        q.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snap) {
                list.clear();
                for (DataSnapshot s : snap.getChildren()) {
                    LeaveModel m = s.getValue(LeaveModel.class);
                    if (m != null) {
                        m.leaveId = s.getKey();
                        list.add(0, m); // latest on top
                    }
                }
                adapter.notifyDataSetChanged();
            }

            @Override public void onCancelled(@NonNull DatabaseError e) {}
        });
    }
}
