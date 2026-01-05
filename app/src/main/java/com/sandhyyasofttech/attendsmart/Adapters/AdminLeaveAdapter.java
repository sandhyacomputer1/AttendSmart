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

        h.tvName.setText(m.employeeName);
        h.tvDates.setText(m.fromDate + " → " + m.toDate);
        h.tvReason.setText("Reason: " + m.reason);
        h.tvStatus.setText(m.status);

        boolean pending = "PENDING".equals(m.status);
        h.btnApprove.setEnabled(pending);
        h.btnReject.setEnabled(pending);

//        h.btnApprove.setOnClickListener(v -> {
//            leavesRef.child(m.leaveId)
//                    .child("status").setValue("APPROVED");
//            leavesRef.child(m.leaveId)
//                    .child("actionAt").setValue(System.currentTimeMillis());
//            Toast.makeText(context,"Approved",Toast.LENGTH_SHORT).show();
//        });
        h.btnApprove.setOnClickListener(v -> {
            showApproveDialog(m);
        });
        h.btnReject.setOnClickListener(v -> showRejectDialog(m.leaveId));
    }
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

            @Override public void onCancelled(@NonNull DatabaseError e) {}
        });
    }
//    private void countUsedPaidLeaves(LeaveModel m, int allowedPaidLeaves) {
//
//        Query q = leavesRef.orderByChild("employeeMobile")
//                .equalTo(m.employeeMobile);
//
//        q.addListenerForSingleValueEvent(new ValueEventListener() {
//            @Override
//            public void onDataChange(@NonNull DataSnapshot snap) {
//
////                int usedPaidLeaves = 0;
//                double usedPaidLeaves = 0;
//
//
//                for (DataSnapshot d : snap.getChildren()) {
//                    LeaveModel lm = d.getValue(LeaveModel.class);
//
//                    if (lm == null) continue;
//
//                    if ("APPROVED".equals(lm.status)
//                            && Boolean.TRUE.equals(lm.isPaid)) {
//
//                        // TODO: month check later (can add)
////                        usedPaidLeaves += lm.leaveType.equals("HALF_DAY") ? 0.5 : 1;
//                          usedPaidLeaves += "HALF_DAY".equals(lm.leaveType) ? 0.5 : 1.0;
//
//                    }
//                }
//
//                showDecisionDialog(m, allowedPaidLeaves, usedPaidLeaves);
//            }
//
//            @Override public void onCancelled(@NonNull DatabaseError e) {}
//        });
//    }
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

                // current approving leave skip
                if (lm.leaveId != null && lm.leaveId.equals(m.leaveId)) continue;

                if (!"APPROVED".equals(lm.status)) continue;
                if (!Boolean.TRUE.equals(lm.isPaid)) continue;
                if (lm.approvedAt == null) continue;

                Calendar cal = Calendar.getInstance();
                cal.setTimeInMillis(lm.approvedAt);

                boolean sameMonth =
                        cal.get(Calendar.MONTH) == now.get(Calendar.MONTH) &&
                                cal.get(Calendar.YEAR) == now.get(Calendar.YEAR);

                if (sameMonth) {
                    usedPaidLeaves +=
                            "HALF_DAY".equals(lm.leaveType) ? 0.5 : 1.0;
                }
            }

            showDecisionDialog(m, allowedPaidLeaves, usedPaidLeaves);
        }

        @Override public void onCancelled(@NonNull DatabaseError e) {}
    });
}

//    private void showDecisionDialog(
//            LeaveModel m,
//            int allowed,
//            int used) {
private void showDecisionDialog(
        LeaveModel m,
        int allowed,
        double used) {

    double remaining = allowed - used;

    String msg =
            "Paid Leaves Allowed: " + allowed +
                    "\nPaid Leaves Used: " + used +
                    "\nRemaining Paid Leaves: " + remaining;

    new AlertDialog.Builder(context)
            .setTitle("Approve Leave")
            .setMessage(msg)
            .setPositiveButton("Approve as PAID", (d,w)-> {

                if (remaining <= 0) {
                    Toast.makeText(context,
                            "No paid leaves remaining",
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                approveLeave(m, true);
            })
            .setNegativeButton("Approve as UNPAID", (d,w)-> {
                approveLeave(m, false);
            })
            .setNeutralButton("Cancel", null)
            .show();
}

    private void approveLeave(LeaveModel m, boolean isPaid) {

        Map<String, Object> map = new HashMap<>();
        map.put("status", "APPROVED");
        map.put("isPaid", isPaid);
        map.put("approvedAt", System.currentTimeMillis());
        String adminName = new PrefManager(context).getUserName();
        map.put("approvedBy", adminName);

        leavesRef.child(m.leaveId).updateChildren(map)
                .addOnSuccessListener(a ->
                        Toast.makeText(context,
                                isPaid ? "Paid Leave Approved" : "Unpaid Leave Approved",
                                Toast.LENGTH_SHORT).show()
                );
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
