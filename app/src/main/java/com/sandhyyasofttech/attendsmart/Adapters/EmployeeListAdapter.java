package com.sandhyyasofttech.attendsmart.Adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.imageview.ShapeableImageView;
import com.sandhyyasofttech.attendsmart.Models.EmployeeModel;
import com.sandhyyasofttech.attendsmart.R;

import java.util.ArrayList;
import java.util.List;

public class EmployeeListAdapter extends RecyclerView.Adapter<EmployeeListAdapter.ViewHolder> implements Filterable {

    private List<EmployeeModel> employeeList;
    private List<EmployeeModel> filteredList;
    private List<EmployeeModel> originalList;
    private EmployeeClickListener clickListener;

    public interface EmployeeClickListener {
        void onEmployeeClick(EmployeeModel employee);
    }

    public EmployeeListAdapter(List<EmployeeModel> employeeList, EmployeeClickListener clickListener) {
        this.originalList = new ArrayList<>();
        this.employeeList = new ArrayList<>();
        this.filteredList = new ArrayList<>();
        this.clickListener = clickListener;
        updateData(employeeList);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_employee_list_view, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        EmployeeModel model = filteredList.get(position);
        holder.bind(model, clickListener);
    }

    @Override
    public int getItemCount() {
        return filteredList.size();
    }

    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                List<EmployeeModel> filteredResults = new ArrayList<>();
                if (constraint == null || constraint.length() == 0) {
                    filteredResults.addAll(originalList);
                } else {
                    String filterPattern = constraint.toString().toLowerCase().trim();
                    for (EmployeeModel item : originalList) {
                        if (matchesFilter(item, filterPattern)) {
                            filteredResults.add(item);
                        }
                    }
                }

                FilterResults results = new FilterResults();
                results.values = filteredResults;
                results.count = filteredResults.size();
                return results;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                filteredList.clear();
                @SuppressWarnings("unchecked")
                List<EmployeeModel> filteredListFromResults = (List<EmployeeModel>) results.values;
                if (filteredListFromResults != null) {
                    filteredList.addAll(filteredListFromResults);
                }
                notifyDataSetChanged();
            }
        };
    }

    public void filter(String query) {
        getFilter().filter(query);
    }

    public void updateData(List<EmployeeModel> newList) {
        if (newList != null) {
            this.originalList.clear();
            this.originalList.addAll(newList);
            this.filteredList.clear();
            this.filteredList.addAll(newList);
            notifyDataSetChanged();
        }
    }

    private boolean matchesFilter(EmployeeModel model, String query) {
        if (model == null || query == null || query.isEmpty()) {
            return false;
        }

        // Safe null checks for all fields
        String name = model.getEmployeeName();
        String id = model.getEmployeeId();
        String mobile = model.getEmployeeMobile();
        String role = model.getEmployeeRole();

        return (name != null && name.toLowerCase().contains(query)) ||
                (id != null && id.toLowerCase().contains(query)) ||
                (mobile != null && mobile.contains(query)) ||
                (role != null && role.toLowerCase().contains(query));
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ShapeableImageView ivAvatar;
        TextView tvName, tvId, tvMobile, tvRole;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.ivAvatar);
            tvName = itemView.findViewById(R.id.tvName);
            tvId = itemView.findViewById(R.id.tvId);
            tvMobile = itemView.findViewById(R.id.tvMobile);
            tvRole = itemView.findViewById(R.id.tvRole);
        }

        public void bind(EmployeeModel model, EmployeeClickListener listener) {
            if (model != null) {
                ivAvatar.setImageResource(R.drawable.ic_profile);
                tvName.setText(safeString(model.getEmployeeName(), "N/A"));
                tvId.setText("ID: " + safeString(model.getEmployeeId(), "N/A"));
                tvMobile.setText(safeString(model.getEmployeeMobile(), "N/A"));
                tvRole.setText(safeString(model.getEmployeeRole(), "Staff"));
            }

            itemView.setOnClickListener(v -> {
                if (listener != null && model != null) {
                    listener.onEmployeeClick(model);
                }
            });
        }

        private String safeString(String value, String defaultValue) {
            return value != null && !value.trim().isEmpty() ? value : defaultValue;
        }
    }
}
