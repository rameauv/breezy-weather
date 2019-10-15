package wangdaye.com.geometricweather.db.controller;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.greenrobot.greendao.query.QueryBuilder;

import java.util.List;

import wangdaye.com.geometricweather.basic.model.option.provider.WeatherSource;
import wangdaye.com.geometricweather.db.entity.DaoSession;
import wangdaye.com.geometricweather.db.entity.LocationEntity;
import wangdaye.com.geometricweather.db.entity.LocationEntityDao;
import wangdaye.com.geometricweather.db.propertyConverter.WeatherSourceConverter;

public class LocationEntityController extends AbsEntityController<LocationEntity> {
    
    public LocationEntityController(DaoSession session) {
        super(session);
    }

    // insert.

    public void insertLocationEntity(@NonNull LocationEntity entity) {
        LocationEntity existInstance = selectLocationEntity(
                entity.cityId, entity.weatherSource, entity.currentPosition);
        if (existInstance == null) {
            getSession().getLocationEntityDao().insert(entity);
        } else {
            entity.id = existInstance.id;
            getSession().getLocationEntityDao().update(entity);
        }
        getSession().clear();
    }

    public void insertLocationEntityList(@NonNull List<LocationEntity> entityList) {
        if (entityList.size() != 0) {
            deleteLocationEntity();
            getSession().getLocationEntityDao().insertInTx(entityList);
            getSession().clear();
        }
    }

    // delete.

    public void deleteLocationEntity(@NonNull LocationEntity entity) {
        LocationEntity existInstance = selectLocationEntity(
                entity.cityId, entity.weatherSource, entity.currentPosition);
        if (existInstance != null) {
            getSession().getLocationEntityDao().delete(existInstance);
            getSession().clear();
        }
    }

    public void deleteLocationEntity() {
        getSession().getLocationEntityDao().deleteAll();
        getSession().clear();
    }

    // select.

    @Nullable
    public LocationEntity selectLocationEntity(@NonNull String cityId, WeatherSource source, boolean currentPosition) {
        QueryBuilder<LocationEntity> builder = getSession().getLocationEntityDao().queryBuilder();
        if (currentPosition) {
            builder.where(LocationEntityDao.Properties.CurrentPosition.eq(true));
        } else {
            builder.where(
                    LocationEntityDao.Properties.CityId.eq(cityId),
                    LocationEntityDao.Properties.WeatherSource.eq(
                            new WeatherSourceConverter().convertToDatabaseValue(source)
                    )
            );
        }
        List<LocationEntity> entityList = builder.list();
        if (entityList == null || entityList.size() <= 0) {
            return null;
        } else {
            return entityList.get(0);
        }
    }

    @NonNull
    public List<LocationEntity> selectLocationEntityList() {
        return getNonNullList(
                getSession().getLocationEntityDao()
                        .queryBuilder()
                        .list()
        );
    }

    public int countLocationEntity() {
        return (int) getSession().getLocationEntityDao()
                .queryBuilder()
                .count();
    }
}
