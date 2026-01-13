package com.sandhyyasofttech.attendsmart.Adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.sandhyyasofttech.attendsmart.Models.LeaveModel;
import com.sandhyyasofttech.attendsmart.R;
import com.sandhyyasofttech.attendsmart.Utils.PrefManager;

import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

        // ✅ NULL SAFE BINDING
        safeSetText(h.tvName, m.employeeName);
        safeSetText(h.tvDates, m.fromDate + " → " + m.toDate);
        safeSetText(h.tvReason, m.reason);
        safeSetText(h.tvStatus, m.status);

        // ✅ HIDE BUTTONS FOR NON-PENDING LEAVES
        boolean isPending = "PENDING".equals(getSafeString(m.status));

        if (isPending) {
            // Show buttons for pending leaves
            h.btnApprove.setVisibility(View.VISIBLE);
            h.btnReject.setVisibility(View.VISIBLE);
            h.btnApprove.setEnabled(true);
            h.btnReject.setEnabled(true);

            // Green color for approve button
            h.btnApprove.setBackgroundColor(context.getResources().getColor(android.R.color.holo_green_dark));
        } else {
            // ✅ HIDE BUTTONS for approved/rejected leaves
            h.btnApprove.setVisibility(View.GONE);
            h.btnReject.setVisibility(View.GONE);
        }

        // Set click listeners (only work when buttons are visible)
        h.btnApprove.setOnClickListener(v -> showApproveDialog(m));
        h.btnReject.setOnClickListener(v -> showRejectDialog(m.leaveId));
    }

    // ✅ HELPER METHODS
    private void safeSetText(TextView tv, String text) {
        if (tv == null) {
            android.util.Log.e("AdminLeaveAdapter", "⚠️ TextView is NULL - Skipping!");
            return;
        }

        if (text == null || text.trim().isEmpty()) {
            tv.setText("N/A");
        } else {
            tv.setText(text);
        }
    }

    private String getSafeString(String value) {
        return value != null ? value : "";
    }

    // ✅ APPROVE LOGIC
    private void showApproveDialog(LeaveModel m) {
        String companyKey = new PrefManager(context).getCompanyKey();

        DatabaseReference salaryRef = FirebaseDatabase.getInstance()
                .getReference("Companies")
                .child(companyKey)
                .child("employees")
                .child(m.employeeMobile)
                .child("salaryConfig")
                .child("paidLeaves");

        salaryRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snap) {
                int allowedPaidLeaves = 0;

                if (snap.exists()) {
                    Object val = snap.getValue();
                    if (val instanceof Long) {
                        allowedPaidLeaves = ((Long) val).intValue();
                    } else if (val instanceof String) {
                        allowedPaidLeaves = Integer.parseInt((String) val);
                    }
                }

                countUsedPaidLeaves(m, allowedPaidLeaves);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError e) {
                Toast.makeText(context, "Error loading leave data", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ✅ COUNT USED PAID LEAVES (CURRENT MONTH ONLY)
    private void countUsedPaidLeaves(LeaveModel m, int allowedPaidLeaves) {
        Query q = leavesRef.orderByChild("employeeMobile")
                .equalTo(m.employeeMobile);

        q.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snap) {
                double usedPaidLeaves = 0;
                Calendar now = Calendar.getInstance();

                for (DataSnapshot d : snap.getChildren()) {
                    LeaveModel lm = d.getValue(LeaveModel.class);
                    if (lm == null) continue;

                    // Skip current leave being approved
                    if (lm.leaveId != null && lm.leaveId.equals(m.leaveId)) continue;

                    if (!"APPROVED".equals(lm.status)) continue;
                    if (!Boolean.TRUE.equals(lm.isPaid)) continue;
                    if (lm.approvedAt == null) continue;

                    // ✅ Check if leave is in current month
                    Calendar cal = Calendar.getInstance();
                    cal.setTimeInMillis(lm.approvedAt);

                    boolean sameMonth =
                            cal.get(Calendar.MONTH) == now.get(Calendar.MONTH) &&
                                    cal.get(Calendar.YEAR) == now.get(Calendar.YEAR);

                    if (sameMonth) {
                        usedPaidLeaves += "HALF_DAY".equals(lm.leaveType) ? 0.5 : 1.0;
                    }
                }

                showDecisionDialog(m, allowedPaidLeaves, usedPaidLeaves);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError e) {
                Toast.makeText(context, "Error counting leaves", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ✅ SHOW DECISION DIALOG
    private void showDecisionDialog(LeaveModel m, int allowed, double used) {
        double remaining = allowed - used;

        // ✅ Calculate days for current leave
        double currentLeaveDays = "HALF_DAY".equals(m.leaveType) ? 0.5 : 1.0;

        String msg = "📊 Leave Balance Information\n\n" +
                "Paid Leaves Allowed: " + allowed + "\n" +
                "Already Used: " + used + "\n" +
                "Remaining: " + remaining + "\n\n" +
                "Current Request: " + currentLeaveDays + " day(s)\n" +
                "Employee: " + m.employeeName;

        new AlertDialog.Builder(context)
                .setTitle("Approve Leave Request")
                .setMessage(msg)
                .setPositiveButton("✅ Approve as PAID", (d, w) -> {
                    if (remaining < currentLeaveDays) {
                        Toast.makeText(context,
                                "⚠️ Not enough paid leaves remaining! Only " + remaining + " available.",
                                Toast.LENGTH_LONG).show();
                        return;
                    }
                    approveLeave(m, true);
                })
                .setNegativeButton("🔴 Approve as UNPAID", (d, w) -> {
                    approveLeave(m, false);
                })
                .setNeutralButton("Cancel", null)
                .show();
    }

    // ✅ APPROVE LEAVE
    private void approveLeave(LeaveModel m, boolean isPaid) {
        Map<String, Object> map = new HashMap<>();
        map.put("status", "APPROVED");
        map.put("isPaid", isPaid);
        map.put("approvedAt", System.currentTimeMillis());

        String adminName = new PrefManager(context).getUserName();
        map.put("approvedBy", adminName);

        leavesRef.child(m.leaveId).updateChildren(map)
                .addOnSuccessListener(a -> {
                    String msg = isPaid ? "✅ Paid Leave Approved" : "⚠️ Unpaid Leave Approved";
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();

                    // ✅ AUTO-HIDE: Firebase listener in Activity will refresh list → buttons auto-hide
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(context, "❌ Failed to approve leave", Toast.LENGTH_SHORT).show();
                });
    }

    // ✅ REJECT LEAVE
    private void showRejectDialog(String leaveId) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.dialog_reject_leave, null);

        EditText et = view.findViewById(R.id.etReason);

        new AlertDialog.Builder(context)
                .setTitle("Reject Leave Request")
                .setView(view)
                .setPositiveButton("Reject", (d, w) -> {
                    String r = et.getText().toString().trim();
                    if (r.isEmpty()) {
                        Toast.makeText(context, "⚠️ Reason required", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    Map<String, Object> updates = new HashMap<>();
                    updates.put("status", "REJECTED");
                    updates.put("adminReason", r);
                    updates.put("actionAt", System.currentTimeMillis());

                    String adminName = new PrefManager(context).getUserName();
                    updates.put("rejectedBy", adminName);

                    leavesRef.child(leaveId).updateChildren(updates)
                            .addOnSuccessListener(a -> {
                                Toast.makeText(context, "❌ Leave Rejected", Toast.LENGTH_SHORT).show();
                                // ✅ AUTO-HIDE: Firebase listener will refresh → buttons auto-hide
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(context, "❌ Failed to reject leave", Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvName, tvDates, tvReason, tvStatus;
        MaterialButton btnApprove, btnReject;

        VH(View v) {
            super(v);

            tvName = v.findViewById(R.id.tvName);
            tvDates = v.findViewById(R.id.tvDates);
            tvReason = v.findViewById(R.id.tvReason);
            tvStatus = v.findViewById(R.id.tvStatus);
            btnApprove = v.findViewById(R.id.btnApprove);
            btnReject = v.findViewById(R.id.btnReject);

            // ✅ DEBUG: Log missing views
            if (tvName == null) android.util.Log.e("AdminLeaveAdapter", "❌ tvName is NULL!");
            if (tvDates == null) android.util.Log.e("AdminLeaveAdapter", "❌ tvDates is NULL!");
            if (tvReason == null) android.util.Log.e("AdminLeaveAdapter", "❌ tvReason is NULL!");
            if (tvStatus == null) android.util.Log.e("AdminLeaveAdapter", "❌ tvStatus is NULL!");
            if (btnApprove == null) android.util.Log.e("AdminLeaveAdapter", "❌ btnApprove is NULL!");
            if (btnReject == null) android.util.Log.e("AdminLeaveAdapter", "❌ btnReject is NULL!");
        }
    }
}
