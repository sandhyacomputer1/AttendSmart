package com.sandhyyasofttech.attendsmart.Adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.DatabaseReference;
import com.sandhyyasofttech.attendsmart.Models.LeaveModel;
import com.sandhyyasofttech.attendsmart.R;

import java.util.List;

public class AdminLeaveAdapter extends RecyclerView.Adapter<AdminLeaveAdapter.VH> {

    private final List<LeaveModel> list;
    private final DatabaseReference leavesRef;
    private final Context context;

    public AdminLeaveAdapter(Context c, List<LeaveModel> l, DatabaseReference ref) {
        context = c;
        list = l;
        leavesRef = ref;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup p, int v) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.row_admin_leave, p, false);
        return new VH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int i) {
        LeaveModel m = list.get(i);

        h.tvName.setText(m.employeeName);
        h.tvDates.setText(m.fromDate + " → " + m.toDate);
        h.tvReason.setText("Reason: " + m.reason);
        h.tvStatus.setText(m.status);

        boolean pending = "PENDING".equals(m.status);
        h.btnApprove.setEnabled(pending);
        h.btnReject.setEnabled(pending);

        h.btnApprove.setOnClickListener(v -> {
            leavesRef.child(m.leaveId)
                    .child("status").setValue("APPROVED");
            leavesRef.child(m.leaveId)
                    .child("actionAt").setValue(System.currentTimeMillis());
            Toast.makeText(context,"Approved",Toast.LENGTH_SHORT).show();
        });

        h.btnReject.setOnClickListener(v -> showRejectDialog(m.leaveId));
    }

    private void showRejectDialog(String leaveId) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.dialog_reject_leave, null);

        EditText et = view.findViewById(R.id.etReason);

        new AlertDialog.Builder(context)
                .setTitle("Reject Leave")
                .setView(view)
                .setPositiveButton("Reject", (d,w)->{
                    String r = et.getText().toString().trim();
                    if (r.isEmpty()) {
                        Toast.makeText(context,"Reason required",Toast.LENGTH_SHORT).show();
                        return;
                    }
                    leavesRef.child(leaveId).child("status").setValue("REJECTED");
                    leavesRef.child(leaveId).child("adminReason").setValue(r);
                    leavesRef.child(leaveId).child("actionAt")
                            .setValue(System.currentTimeMillis());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override public int getItemCount() { return list.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvName,tvDates,tvReason,tvStatus;
        MaterialButton btnApprove,btnReject;
        VH(View v){
            super(v);
            tvName=v.findViewById(R.id.tvName);
            tvDates=v.findViewById(R.id.tvDates);
            tvReason=v.findViewById(R.id.tvReason);
            tvStatus=v.findViewById(R.id.tvStatus);
            btnApprove=v.findViewById(R.id.btnApprove);
            btnReject=v.findViewById(R.id.btnReject);
        }
    }
}
