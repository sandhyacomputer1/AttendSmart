package com.sandhyyasofttech.attendsmart.Adapters;

import android.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.sandhyyasofttech.attendsmart.Models.DepartmentModel;
import com.sandhyyasofttech.attendsmart.R;

import java.util.List;

public class DepartmentAdapter extends RecyclerView.Adapter<DepartmentAdapter.ViewHolder> {

    private final List<DepartmentModel> list;
    private final OnDepartmentActionListener listener;

    public interface OnDepartmentActionListener {
        void onEditDepartment(String oldName, String newName, int position);
        void onDeleteDepartment(String departmentName, int position);
    }

    public DepartmentAdapter(List<DepartmentModel> list, OnDepartmentActionListener listener) {
        this.list = list;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_department, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DepartmentModel department = list.get(position);
        holder.tvDepartmentName.setText(department.name);

        holder.btnEdit.setOnClickListener(v -> {
            int adapterPosition = holder.getAdapterPosition();
            if (adapterPosition != RecyclerView.NO_POSITION) {
                showEditDialog(v, list.get(adapterPosition).name, adapterPosition);
            }
        });

        holder.btnDelete.setOnClickListener(v -> {
            int adapterPosition = holder.getAdapterPosition();
            if (adapterPosition != RecyclerView.NO_POSITION) {
                showDeleteConfirmation(v, list.get(adapterPosition).name, adapterPosition);
            }
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    private void showEditDialog(View view, String currentName, int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(view.getContext());
        View dialogView = LayoutInflater.from(view.getContext())
                .inflate(R.layout.dialog_edit_department, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        TextInputEditText etDepartmentName = dialogView.findViewById(R.id.etDepartmentName);
        MaterialButton btnCancel = dialogView.findViewById(R.id.btnCancel);
        MaterialButton btnUpdate = dialogView.findViewById(R.id.btnUpdate);

        etDepartmentName.setText(currentName);
        etDepartmentName.setSelection(currentName.length());

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnUpdate.setOnClickListener(v -> {
            String newName = etDepartmentName.getText().toString().trim();

            if (newName.isEmpty()) {
                Toast.makeText(view.getContext(), "Department name cannot be empty", Toast.LENGTH_SHORT).show();
                return;
            }

            if (newName.equals(currentName)) {
                Toast.makeText(view.getContext(), "No changes made", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
                return;
            }

            if (listener != null) {
                listener.onEditDepartment(currentName, newName, position);
            }

            dialog.dismiss();
        });

        dialog.show();
    }

    private void showDeleteConfirmation(View view, String departmentName, int position) {
        new AlertDialog.Builder(view.getContext())
                .setTitle(" Permanent Delete")
                .setMessage("Are you sure you want to PERMANENTLY delete \"" + departmentName)
                .setIcon(R.drawable.ic_delete)
                .setPositiveButton("Permanent Delete", (dialog, which) -> {
                    if (listener != null) {
                        listener.onDeleteDepartment(departmentName, position);
                    }
                })
                .setNegativeButton("Cancel", null)
                .setCancelable(false)  // Prevent accidental dismiss
                .show();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDepartmentName;
        MaterialButton btnEdit, btnDelete;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDepartmentName = itemView.findViewById(R.id.tvDepartmentName);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}
