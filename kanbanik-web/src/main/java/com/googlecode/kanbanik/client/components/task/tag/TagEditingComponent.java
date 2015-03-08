package com.googlecode.kanbanik.client.components.task.tag;

import com.googlecode.kanbanik.client.components.PanelContainingDialog;

public class TagEditingComponent extends BaseTagEditingComponent {

    @Override
    protected void edit() {
        name.setText(getDto().getName());
        description.setText(getDto().getDescription());
        pictureUrl.setText(getDto().getPictureUrl());
        onClickUrl.setText(getDto().getOnClickUrl());
        color.setText(getDto().getColour());
    }

    @Override
    public void okClicked(PanelContainingDialog dialog) {
        getParentWidget().editItem(doFlush(getDto()));
        super.okClicked(dialog);
    }
}
