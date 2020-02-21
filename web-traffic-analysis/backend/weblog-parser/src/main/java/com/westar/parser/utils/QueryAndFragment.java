package com.westar.parser.utils;

public class QueryAndFragment {
    //查询条件
    private String query;
    private String fragment;

    public QueryAndFragment(String query, String fragment) {
        this.query = query;
        this.fragment = fragment;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getFragment() {
        return fragment;
    }

    public void setFragment(String fragment) {
        this.fragment = fragment;
    }
}
