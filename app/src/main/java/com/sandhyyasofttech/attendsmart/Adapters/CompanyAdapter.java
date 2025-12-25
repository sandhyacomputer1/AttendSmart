package com.sandhyyasofttech.attendsmart.Adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.sandhyyasofttech.attendsmart.Models.CompanyItem;
import com.sandhyyasofttech.attendsmart.R;

import java.util.List;
import java.util.function.Consumer;

public class CompanyAdapter extends RecyclerView.Adapter<CompanyAdapter.CompanyViewHolder> {
    
    private List<CompanyItem> companies;
    private Consumer<CompanyItem> onCompanyClick;

    public CompanyAdapter(List<CompanyItem> companies, Consumer<CompanyItem> onCompanyClick) {
        this.companies = companies;
        this.onCompanyClick = onCompanyClick;
    }

    @NonNull
    @Override
    public CompanyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_company, parent, false);
        return new CompanyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CompanyViewHolder holder, int position) {
        CompanyItem company = companies.get(position);
        holder.tvCompanyName.setText(company.companyName);
        holder.itemView.setOnClickListener(v -> onCompanyClick.accept(company));
    }

    @Override
    public int getItemCount() {
        return companies.size();
    }

    static class CompanyViewHolder extends RecyclerView.ViewHolder {
        TextView tvCompanyName;

        CompanyViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCompanyName = itemView.findViewById(R.id.tvCompanyName);
        }
    }
}
