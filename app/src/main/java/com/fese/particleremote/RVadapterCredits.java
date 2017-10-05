package com.fese.particleremote;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

import static com.fese.particleremote.RVadapterCredits.CreditViewHolder.mCreditClickListener;

/**
 * Created by Fabian on 15-Apr-17.
 */


class Credit {
    String creditTitle;
    String creditAuthor;
    String creditLink;
    String creditLicence;




    Credit(String creditTitle, String creditAuthor, String creditLink, String creditLicence) {
        this.creditTitle = creditTitle;
        this.creditAuthor = creditAuthor;
        this.creditLink = creditLink;
        this.creditLicence = creditLicence;

    }
}

public class RVadapterCredits extends RecyclerView.Adapter<RVadapterCredits.CreditViewHolder> {

    public interface OnCreditClickedListener {
        void onCreditClicked(String creditLink);
    }

    public static class CreditViewHolder extends RecyclerView.ViewHolder {
        TextView creditTitle;
        TextView creditAuthor;
        TextView creditLink;
        TextView creditLicence;
        View view;


        static OnCreditClickedListener mCreditClickListener;
        public static void setOnCreditClickedListener(OnCreditClickedListener l) {
            mCreditClickListener = l;
        }


        public CreditViewHolder(View itemView) {
            super(itemView);

            creditTitle = (TextView) itemView.findViewById(R.id.tv_creditTitle);
            creditAuthor = (TextView) itemView.findViewById(R.id.tv_creditAuthor);
            creditLink = (TextView) itemView.findViewById(R.id.tv_creditLink);
            creditLicence = (TextView) itemView.findViewById(R.id.tv_creditLicence);
            view = itemView;

        }



    }


    static List<Credit> creditList;

    RVadapterCredits(List<Credit> creditList){
        this.creditList = creditList;
    }



    public RVadapterCredits.CreditViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.cardview_credit, viewGroup, false);
        return new RVadapterCredits.CreditViewHolder(v);

    }

    @Override
    public void onBindViewHolder(RVadapterCredits.CreditViewHolder holder, int position) {
        final Credit credit = creditList.get(position);
        holder.creditTitle.setText(credit.creditTitle);
        holder.creditAuthor.setText("Copyright © " + credit.creditAuthor);
        holder.creditLink.setText(credit.creditLink);
        holder.creditLicence.setText(credit.creditLicence);

        holder.view.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                if (mCreditClickListener != null) {
                    mCreditClickListener.onCreditClicked(credit.creditLink);

                }

            }
        });
    }



    @Override
    public int getItemCount() {
        return creditList.size();
    }


}
