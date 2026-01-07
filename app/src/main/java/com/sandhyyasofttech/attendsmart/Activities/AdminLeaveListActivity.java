package com.sandhyyasofttech.attendsmart.Activities;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.sandhyyasofttech.attendsmart.Adapters.AdminLeaveAdapter;
import com.sandhyyasofttech.attendsmart.Adapters.MyLeavesAdapter;
import com.sandhyyasofttech.attendsmart.Models.LeaveModel;
import com.sandhyyasofttech.attendsmart.R;
import com.sandhyyasofttech.attendsmart.Utils.PrefManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
public class AdminLeaveListActivity extends AppCompatActivity {

    private RecyclerView rv;
    private final List<LeaveModel> list = new ArrayList<>();
    private DatabaseReference leavesRef;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_admin_leave_list);

        // 🔹 Toolbar setup
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("All Leaves");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            toolbar.setTitleTextColor(getResources().getColor(android.R.color.white, getTheme()));
        }

// 🔹 Make back arrow WHITE
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back);
        toolbar.getNavigationIcon().setTint(
                getResources().getColor(android.R.color.white)
        );

        toolbar.setNavigationOnClickListener(v -> finish());
        // 🔹 RecyclerView
        rv = findViewById(R.id.rvLeaves);
        rv.setLayoutManager(new LinearLayoutManager(this));

        String companyKey = new PrefManager(this).getCompanyKey();
        leavesRef = FirebaseDatabase.getInstance()
                .getReference("Companies")
                .child(companyKey)
                .child("leaves");

        AdminLeaveAdapter adapter =
                new AdminLeaveAdapter(this, list, leavesRef);
        rv.setAdapter(adapter);

        leavesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot s) {
                list.clear();
                for (DataSnapshot d : s.getChildren()) {
                    LeaveModel m = d.getValue(LeaveModel.class);
                    if (m != null) {
                        m.leaveId = d.getKey();
                        list.add(m);
                    }
                }

                // 🔹 Pending → Approved → Rejected order
                Collections.sort(list, (a, b1) -> {
                    if (a.status == null && b1.status == null) return 0;
                    if (a.status == null) return 1;
                    if (b1.status == null) return -1;

                    if (a.status.equals(b1.status)) return 0;
                    if ("PENDING".equals(a.status)) return -1;
                    if ("PENDING".equals(b1.status)) return 1;
                    if ("APPROVED".equals(a.status)) return -1;
                    return 1;
                });

                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError e) {}
        });
    }
}
