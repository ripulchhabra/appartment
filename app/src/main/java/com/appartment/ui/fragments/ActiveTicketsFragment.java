package com.appartment.ui.fragments;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.appartment.R;
import com.appartment.adapter.TicketAdapter;
import com.appartment.app.AppConfig;
import com.appartment.app.AppController;
import com.appartment.helpers.EndlessRecyclerViewScrollListener;
import com.appartment.helpers.JsonParser;
import com.appartment.helpers.RecyclerItemClickListener;
import com.appartment.helpers.SessionManager;
import com.appartment.model.Ticket;
import com.appartment.ui.ComplaintDetails;
import com.appartment.ui.FilterActivity;
import com.appartment.ui.ListTickets;
import com.appartment.ui.TicketsResultActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ActiveTicketsFragment extends Fragment {
    private Activity activity;
    private final String TAG = ActiveTicketsFragment.class.getSimpleName();
    private RecyclerView recyclerView;
    private TicketAdapter adapter;
    private List<Ticket> ticketList;
    private ProgressBar progressBar;
    private ProgressDialog progressDialog;
    public final static String serialisedObjKey = "TicketObject";
    private SessionManager session;
    private View rootView;

    public ActiveTicketsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        session = new SessionManager(context);
        activity = getActivity();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        setHasOptionsMenu(true);

        if (rootView == null) {
            // Inflate the layout for this fragment
            rootView = inflater.inflate(R.layout.fragment_active_tickets, container, false);

            initViews(rootView);
        } else {
            ViewGroup parent = (ViewGroup) rootView.getParent();
            if (parent != null) {
                parent.removeView(rootView);
            }
        }
        return rootView;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        menu.clear();
        inflater.inflate(R.menu.ticket_menu, menu);
        MenuItem item = menu.findItem(R.id.action_search);

        SearchView searchView = new SearchView(((ListTickets) activity).getSupportActionBar().getThemedContext());

        MenuItemCompat.setShowAsAction(item, MenuItemCompat.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW | MenuItemCompat.SHOW_AS_ACTION_IF_ROOM);
        MenuItemCompat.setActionView(item, searchView);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {

                Bundle bundle = new Bundle();
                bundle.putString(TicketsResultActivity.SEARCH_KEY, query);
                bundle.putString(TicketsResultActivity.TYPE, "active");
                TicketsResultActivity.start(activity, bundle);

                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });
        searchView.setOnClickListener(new View.OnClickListener() {
                                          @Override
                                          public void onClick(View v) {

                                          }
                                      }
        );
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.action_filter:

                Bundle bundle = new Bundle();
                bundle.putString(FilterActivity.TYPE, "active");
                FilterActivity.start(activity, bundle);

                break;

            default:
                break;
        }
        return true;
    }

    private void initViews(View view) {
        recyclerView = (RecyclerView) view.findViewById(R.id.complaintListView);
        progressBar = (ProgressBar) view.findViewById(R.id.progressBar);
        ticketList = new ArrayList<>();
        adapter = new TicketAdapter(getActivity(), ticketList);

        RecyclerView.LayoutManager mLayoutManager = new GridLayoutManager(getActivity(), 1);
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.addOnScrollListener(new EndlessRecyclerViewScrollListener((GridLayoutManager) mLayoutManager) {
            @Override
            public void onLoadMore(int page, int totalItemsCount) {
                //feed data
//                Log.d(TAG,"Page "+ page);
//                Log.d(TAG,"Total "+ totalItemsCount);
//                progressBar.setVisibility(View.VISIBLE);
//                prepareData();
            }
        });
        recyclerView.setAdapter(adapter);

        showDialog();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (progressDialog.isShowing()) {
                    hideDialog();
                    Toast.makeText(getActivity(), "connection error", Toast.LENGTH_LONG).show();
                }
            }
        }, 5000);
        prepareData();

        recyclerView.addOnItemTouchListener(
                new RecyclerItemClickListener(getActivity(), new RecyclerItemClickListener.OnItemClickListener() {
                    @Override
                    public void onItemClick(View v, int position) {
                        Intent intent = new Intent(getActivity(), ComplaintDetails.class);
                        intent.putExtra(serialisedObjKey, ticketList.get(position));
                        startActivity(intent);
                    }
                })
        );
    }

    private void prepareData() {

        String tag_string_req = "ticket_request";

        final String userId = Integer.toString(session.getUserId());
        final String status = "Active";

        StringRequest strReq = new StringRequest(Request.Method.POST,
                AppConfig.URL_TICKETS, new Response.Listener<String>() {

            @Override
            public void onResponse(String response) {
                Log.d(TAG, "Tickets: " + response.toString());
                try {
                    JSONObject jObj = new JSONObject(response);
                    int status = jObj.getInt("status");

                    // Check for status node in json
                    if (status == 200) {
                        // successful
                        onSuccess(response);
                    } else {
                        // Error Get the error message
                        String errorMsg = jObj.getString("message");
                        onFailure(errorMsg);
                    }
                } catch (JSONException e) {
                    // JSON error
                    e.printStackTrace();
                }
                hideDialog();
            }
        }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(TAG, "Login Error: " + error.getMessage());
                onFailure("something went wrong. please try again");
                hideDialog();
            }
        }) {

            @Override
            protected Map<String, String> getParams() {
                // Posting parameters to login url
                Map<String, String> params = new HashMap<String, String>();
                params.put("user_id", userId);
                params.put("status", status);
                return params;
            }

        };

        // Adding request to request queue
        AppController.getInstance().addToRequestQueue(strReq, tag_string_req);
    }

    private void onFailure(String message) {
        Toast.makeText(getActivity(), message, Toast.LENGTH_LONG).show();
    }

    private void setNotFoundLayout(String message) {
        // set not found message layout
    }

    private void onSuccess(String response) {
        JsonParser.parseTickets(response, ticketList);
        hideDialog();
        adapter.notifyDataSetChanged();
    }

    private void showDialog() {
        // show onscreen loader first time only
        progressDialog = new ProgressDialog(getActivity());
        progressDialog.setCancelable(false);
        progressDialog.setIndeterminate(true);
        progressDialog.setMessage("Loading..");
        progressDialog.show();
    }

    private void hideDialog() {
        if (progressDialog.isShowing())
            progressDialog.dismiss();
    }

}
