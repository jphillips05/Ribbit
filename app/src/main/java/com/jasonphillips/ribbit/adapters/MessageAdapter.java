package com.jasonphillips.ribbit.adapters;

import android.content.Context;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.jasonphillips.ribbit.Constants;
import com.jasonphillips.ribbit.R;
import com.parse.ParseObject;

import java.util.Date;
import java.util.List;

/**
 * Created by jasonphillips on 3/25/15.
 */
public class MessageAdapter extends ArrayAdapter<ParseObject> {

    private Context mContext;
    private List<ParseObject> mMessages;


    public MessageAdapter(Context context, List<ParseObject> messages) {
        super(context, R.layout.message_item, messages);
        mContext = context;
        mMessages = messages;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            convertView = LayoutInflater.from(mContext).inflate(R.layout.message_item, null);

            holder = new ViewHolder();
            holder.iconImageView = (ImageView) convertView.findViewById(R.id.message_icon);
            holder.nameLabel = (TextView) convertView.findViewById(R.id.sender_label);
            holder.timeLabel = (TextView) convertView.findViewById(R.id.message_date);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        ParseObject message = mMessages.get(position);


        if (message.get(Constants.PARSE_KEY_MESSAGE_FILE_TYPE).equals(Constants.TYPE_IMAGE)) {
            holder.iconImageView.setImageResource(R.drawable.ic_picture);
        } else {
            holder.iconImageView.setImageResource(R.drawable.ic_video);
        }

        holder.nameLabel.setText(message.getString(Constants.PARSE_KEY_MESSAGE_SENDER_NAME));

        Date createdAt = message.getCreatedAt();
        Long now = new Date().getTime();

        holder.timeLabel.setText(
            DateUtils.getRelativeTimeSpanString(
                createdAt.getTime(),
                now,
                DateUtils.SECOND_IN_MILLIS).toString());

        return convertView;
    }

    private static class ViewHolder {
        ImageView iconImageView;
        TextView nameLabel;
        TextView timeLabel;
    }

    public void Refill(List<ParseObject> messages) {
        mMessages.clear();
        mMessages.addAll(messages);
        notifyDataSetChanged();
    }
}
