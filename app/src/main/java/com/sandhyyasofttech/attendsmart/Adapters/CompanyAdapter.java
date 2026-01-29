package com.sandhyyasofttech.attendsmart.Adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.sandhyyasofttech.attendsmart.Models.CompanyItem;
import com.sandhyyasofttech.attendsmart.R;

import java.util.List;

public class CompanyAdapter extends RecyclerView.Adapter<CompanyAdapter.ViewHolder> {

    private final List<CompanyItem> companies;
    private final CompanyClickListener listener;

    public interface CompanyClickListener {
        void onCompanyClick(CompanyItem company);
    }

    public CompanyAdapter(List<CompanyItem> companies, CompanyClickListener listener) {
        this.companies = companies;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_company, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CompanyItem company = companies.get(position);

        holder.tvCompanyName.setText(company.companyName);
        holder.tvCompanyEmail.setText(company.companyEmail);
        holder.tvCompanyPhone.setText(company.companyPhone);

        // Set company initial icon
        if (!company.companyName.isEmpty()) {
            String initial = company.companyName.substring(0, 1).toUpperCase();
            holder.tvCompanyInitial.setText(initial);
        }

        holder.cardCompany.setOnClickListener(v -> {
            if (listener != null) {
                listener.onCompanyClick(company);
            }
        });
    }

    @Override
    public int getItemCount() {
        return companies.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        CardView cardCompany;
        TextView tvCompanyInitial;
        TextView tvCompanyName;
        TextView tvCompanyEmail;
        TextView tvCompanyPhone;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            cardCompany = itemView.findViewById(R.id.cardCompany);
            tvCompanyInitial = itemView.findViewById(R.id.tvCompanyInitial);
            tvCompanyName = itemView.findViewById(R.id.tvCompanyName);
            tvCompanyEmail = itemView.findViewById(R.id.tvCompanyEmail);
            tvCompanyPhone = itemView.findViewById(R.id.tvCompanyPhone);
        }
    }
}