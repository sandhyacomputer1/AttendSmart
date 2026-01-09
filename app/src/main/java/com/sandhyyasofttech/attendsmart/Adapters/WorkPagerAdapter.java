package com.sandhyyasofttech.attendsmart.Adapters;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.sandhyyasofttech.attendsmart.Fragments.WorkHistoryFragment;
import com.sandhyyasofttech.attendsmart.Models.WorkSummary;
import java.util.List;

public class WorkPagerAdapter extends FragmentStateAdapter {
    private List<WorkSummary> works;
    
    public WorkPagerAdapter(@NonNull FragmentActivity fragmentActivity, List<WorkSummary> works) {
        super(fragmentActivity);
        this.works = works;
    }
    
    @NonNull
    @Override
    public Fragment createFragment(int position) {
        return WorkHistoryFragment.newInstance(works.get(position));
    }
    
    @Override
    public int getItemCount() {
        return works != null ? works.size() : 0;
    }
}
