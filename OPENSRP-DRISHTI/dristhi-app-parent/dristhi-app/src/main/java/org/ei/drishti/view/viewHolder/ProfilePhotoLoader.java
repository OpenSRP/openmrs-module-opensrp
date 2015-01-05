package org.ei.drishti.view.viewHolder;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.ei.drishti.AllConstants.DEFAULT_WOMAN_IMAGE_PLACEHOLDER_PATH;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.ei.drishti.view.contract.SmartRegisterClient;

import android.content.res.Resources;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

public class ProfilePhotoLoader {
    private final Resources resources;
    private final Drawable defaultPlaceHolder;
    private final Map<String, Drawable> drawableMap = new HashMap<String, Drawable>();

    public ProfilePhotoLoader(Resources res, Drawable placeHolder) {
        this.resources = res;
        defaultPlaceHolder = placeHolder;
    }

    public Drawable get(SmartRegisterClient client) {
        if (drawableMap.containsKey(client.entityId())) {
            return drawableMap.get(client.entityId());
        }

        String photoPath = client.profilePhotoPath();
        if (isBlank(photoPath)
                || isThisDefaultProfilePhoto(photoPath)
                || !isFileExists(photoPath)) {
            return defaultPlaceHolder;
        }

        Drawable profilePhoto = new BitmapDrawable(resources, photoPath.replace("file:///", "/"));
        drawableMap.put(client.entityId(), profilePhoto);
        return profilePhoto;
    }

    private boolean isFileExists(String path) {
        return new File(path).exists();
    }

    private boolean isThisDefaultProfilePhoto(String photoPath) {
        return photoPath.contains(DEFAULT_WOMAN_IMAGE_PLACEHOLDER_PATH);
    }
}
