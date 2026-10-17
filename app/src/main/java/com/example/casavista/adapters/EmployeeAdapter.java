package com.example.casavista.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.RecyclerView;

import com.example.casavista.R;
import com.example.casavista.models.Employee;

import java.util.ArrayList;
import java.util.List;

public class EmployeeAdapter extends RecyclerView.Adapter<EmployeeAdapter.ViewHolder> {

    public interface OnEmployeeActionListener {
        void onEditRole(Employee employee, int position);
        void onDeleteRole(Employee employee, int position);
    }

    private final List<Employee> employeeList;
    private final OnEmployeeActionListener listener;

    public EmployeeAdapter(List<Employee> employeeList, OnEmployeeActionListener listener) {
        this.employeeList = new ArrayList<>(employeeList);
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_employee, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Employee emp = employeeList.get(position);

        String displayTitle = emp.getName() + " (" + emp.getStaffId() + ")";
        holder.tvEmployeeName.setText(displayTitle);

        String subInfo = emp.getDepartment() + " • " + emp.getEmail() + " (User: " + emp.getUsername() + ")";
        holder.tvDepartment.setText(subInfo);
        holder.tvRoleBadge.setText(emp.getRole());

        if (emp.getName() != null && !emp.getName().isEmpty()) {
            holder.tvAvatarInitial.setText(String.valueOf(emp.getName().charAt(0)).toUpperCase());
        }

        holder.itemView.setOnClickListener(v -> {
            Toast.makeText(
                    v.getContext(),
                    "Login: " + emp.getEmail() + " | Username: " + emp.getUsername() + " | Pass: " + emp.getPassword(),
                    Toast.LENGTH_LONG
            ).show();
        });

        holder.btnMoreOptions.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(v.getContext(), holder.btnMoreOptions);
            popup.getMenu().add(0, 1, 0, "Edit Role & Credentials");
            popup.getMenu().add(0, 2, 1, "Show Password");
            popup.getMenu().add(0, 3, 2, "Remove Staff Account");

            popup.setOnMenuItemClickListener(item -> {
                int adapterPos = holder.getBindingAdapterPosition();
                if (adapterPos != RecyclerView.NO_POSITION && listener != null) {
                    if (item.getItemId() == 1) {
                        listener.onEditRole(emp, adapterPos);
                    } else if (item.getItemId() == 2) {
                        Toast.makeText(
                                v.getContext(),
                                emp.getName() + " → Email: " + emp.getEmail() + " | Pass: " + emp.getPassword(),
                                Toast.LENGTH_LONG
                        ).show();
                    } else if (item.getItemId() == 3) {
                        listener.onDeleteRole(emp, adapterPos);
                    }
                }
                return true;
            });
            popup.show();
        });
    }

    @Override
    public int getItemCount() {
        return employeeList.size();
    }

    public void updateData(List<Employee> newList) {
        this.employeeList.clear();
        this.employeeList.addAll(newList);
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvEmployeeName, tvDepartment, tvRoleBadge, tvAvatarInitial;
        ImageButton btnMoreOptions;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvEmployeeName = itemView.findViewById(R.id.tvEmployeeName);
            tvDepartment = itemView.findViewById(R.id.tvDepartment);
            tvRoleBadge = itemView.findViewById(R.id.tvRoleBadge);
            tvAvatarInitial = itemView.findViewById(R.id.tvAvatarInitial);
            btnMoreOptions = itemView.findViewById(R.id.btnMoreOptions);
        }
    }
}