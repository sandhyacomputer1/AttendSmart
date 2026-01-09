package com.sandhyyasofttech.attendsmart.Fragments;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.database.FirebaseDatabase;
import com.sandhyyasofttech.attendsmart.Activities.EmployeeAllWorksActivity;
import com.sandhyyasofttech.attendsmart.Activities.EmployeeTodayWorkActivity;
import com.sandhyyasofttech.attendsmart.Models.WorkSummary;
import com.sandhyyasofttech.attendsmart.R;
import com.sandhyyasofttech.attendsmart.Utils.PrefManager;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class WorkHistoryFragment extends Fragment {
    private static final String ARG_WORK = "work";
    private WorkSummary work;

    public static WorkHistoryFragment newInstance(WorkSummary work) {
        WorkHistoryFragment fragment = new WorkHistoryFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_WORK, work);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            work = getArguments().getParcelable(ARG_WORK);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_work_history, container, false);

        // 🔥 SAFE NULL CHECKS
        TextView tvDate = rootView.findViewById(R.id.tvDate);
        TextView tvWorkSummary = rootView.findViewById(R.id.tvWorkSummary);
        TextView tvEmployeeName = rootView.findViewById(R.id.tvEmployeeName);
        TextView tvTasks = rootView.findViewById(R.id.tvTasks);
        TextView tvIssues = rootView.findViewById(R.id.tvIssues);
        TextView tvSubmittedTime = rootView.findViewById(R.id.tvSubmittedTime);
        MaterialButton btnEdit = rootView.findViewById(R.id.btnEdit);
        MaterialButton btnDelete = rootView.findViewById(R.id.btnDelete);

        // 🔥 SAFE BINDING
        bindData(tvDate, tvWorkSummary, tvEmployeeName, tvTasks, tvIssues, tvSubmittedTime);

        // 🔥 PROFESSIONAL BUTTONS
        setupEditButton(btnEdit);
        setupDeleteButton(btnDelete);

        return rootView;
    }

    private void bindData(TextView tvDate, TextView tvWorkSummary, TextView tvEmployeeName,
                          TextView tvTasks, TextView tvIssues, TextView tvSubmittedTime) {
        if (work == null) return;

        if (tvDate != null) tvDate.setText(work.workDate != null ? work.workDate : "No Date");
        if (tvWorkSummary != null) tvWorkSummary.setText(work.workSummary != null ? work.workSummary : "No Summary");
        if (tvEmployeeName != null) tvEmployeeName.setText(work.employeeName != null ? work.employeeName : "Unknown");
        if (tvTasks != null) tvTasks.setText("Tasks: " + (work.tasks != null && !work.tasks.isEmpty() ? work.tasks : "None"));
        if (tvIssues != null) tvIssues.setText("Issues: " + (work.issues != null && !work.issues.isEmpty() ? work.issues : "None"));
        if (tvSubmittedTime != null) tvSubmittedTime.setText("Submitted: " + safeFormatTime(work.submittedAt));
    }

    private void setupEditButton(MaterialButton btnEdit) {
        if (btnEdit == null || work == null) return;

        btnEdit.setOnClickListener(v -> {
            if (isTodayWork()) {
                // ✅ PROFESSIONAL EDIT INTENT
                Intent editIntent = new Intent(getActivity(), EmployeeTodayWorkActivity.class);
                editIntent.putExtra("editMode", true);
                editIntent.putExtra("workData", work);
                startActivity(editIntent);
            } else {
                showSimpleToast("✏️ फक्त आजचाच edit होऊ शकतो");
            }
        });
    }

    private void setupDeleteButton(MaterialButton btnDelete) {
        if (btnDelete == null || work == null) return;

        btnDelete.setOnClickListener(v -> {
            if (isTodayWork()) {
                showDeleteConfirmationDialog(btnDelete);
            } else {
                showSimpleToast("🗑️ फक्त आजचाच delete होऊ शकतो");
            }
        });
    }

    // 🔥 DELETE CONFIRMATION DIALOG
    private void showDeleteConfirmationDialog(MaterialButton btnDelete) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("🗑️ Delete Work")
                .setMessage("आजचा कामाचा सारांश delete कराल?\n\nया action ला undo करता येणार नाही!")
                .setPositiveButton("Delete", (dialog, which) -> deleteWork(btnDelete))
                .setNegativeButton("Cancel", null)
                .show();
    }

    // 🔥 FIREBASE DELETE
    private void deleteWork(MaterialButton btnDelete) {
        showLoading(btnDelete, "Deleting...");

        PrefManager pref = new PrefManager(requireContext());
        String companyKey = pref.getCompanyKey();
        String employeeMobile = pref.getEmployeeMobile();

        FirebaseDatabase.getInstance()
                .getReference("Companies")
                .child(companyKey)
                .child("dailyWork")
                .child(work.workDate)
                .child(employeeMobile)
                .removeValue()
                .addOnSuccessListener(aVoid -> {
                    showSimpleToast("✅ Work deleted successfully!");

                    // 🔥 FIXED - Just go back (auto refreshes list)
                    requireActivity().onBackPressed();
                })
                .addOnFailureListener(e -> {
                    showSimpleToast("❌ Delete failed: " + e.getMessage());
                    resetButton(btnDelete);
                });
    }

    private void showLoading(MaterialButton btnDelete, String text) {
        if (btnDelete != null) {
            btnDelete.setText(text);
            btnDelete.setEnabled(false);
        }
    }

    private void resetButton(MaterialButton btnDelete) {
        if (btnDelete != null) {
            btnDelete.setText("🗑️ Delete");
            btnDelete.setEnabled(true);
        }
    }

    // 🔥 UTILITY METHODS
    private boolean isTodayWork() {
        if (work == null || work.workDate == null) return false;
        return work.workDate.equals(getTodayDate());
    }

    private String getTodayDate() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
    }

    private String safeFormatTime(long timestamp) {
        try {
            return new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(new Date(Math.max(timestamp, 0)));
        } catch (Exception e) {
            return "Unknown time";
        }
    }

    private void showSimpleToast(String message) {
        if (getActivity() != null) {
            Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show();
        }
    }
}
