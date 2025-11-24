package com.platinumacre.realestateapp.filters;

import android.widget.Filter;

import com.platinumacre.realestateapp.adapters.AdapterChats;
import com.platinumacre.realestateapp.models.ModelChats;

import java.util.ArrayList;

public class FilterChats extends Filter {
    //declaring AdapterChats and ArrayList<ModelChats> instance that will be initialized in constructor of this class
    private AdapterChats adapter;
    private ArrayList<ModelChats> filterList;

    /**
     * Filter Chats Constructor
     *
     * @param adapter    AdapterChats instance to be passed when this constructor is created
     * @param filterList chats arraylist to be passed when this constructor is created
     */
    public FilterChats(AdapterChats adapter, ArrayList<ModelChats> filterList) {
        this.adapter = adapter;
        this.filterList = filterList;
    }

    @Override
    protected FilterResults performFiltering(CharSequence constraint) {
        //perform filter based on what user type
        FilterResults results = new FilterResults();

        if (constraint != null && constraint.length() > 0){
            //the search query is not null and not empty, we can perform filter, convert the typed query to upper case
            //to make search not case sensitive e.g. Atif Pervaiz -> ATIF PERVAIZ
            constraint = constraint.toString().toUpperCase();
            //hold the filtered list of Ads based on user searched query
            ArrayList<ModelChats> filteredModels = new ArrayList<>();
            for (int i=0; i<filterList.size(); i++){
                //Ad filter based on Receipt User Name. If matches add it to the filteredModels list
                if (filterList.get(i).getName().toUpperCase().contains(constraint)){
                    //Filter matched add to filteredModels list
                    filteredModels.add(filterList.get(i));
                }
            }
            //the search query has matched item(s), we can perform filter. Return filteredModels list
            results.count = filteredModels.size();
            results.values = filteredModels;
        } else {
            //the search query is either null or empty, we can't perform filter. Return full/original list
            results.count = filterList.size();
            results.values = filterList;
        }

        return results;
    }

    @Override
    protected void publishResults(CharSequence constraint, FilterResults results) {

        //publish the filtered result
        adapter.chatsArrayList = (ArrayList<ModelChats>) results.values;

        adapter.notifyDataSetChanged();
    }

}
