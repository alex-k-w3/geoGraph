CREATE TABLE geo_objects(
	uid     uuid NOT NULL,
	object_type varchar(40) NOT NULL, -- poi, region
	osm_id  bigint NULL,
    PRIMARY KEY (uid)
);

CREATE INDEX geo_objects_osm_id ON geo_objects(osm_id);

CREATE TABLE geo_regions(
	uid         uuid NOT NULL,
	region        geography(MultiPolygon) NOT NULL,
    PRIMARY KEY (uid),
    FOREIGN KEY (uid) REFERENCES geo_objects(uid)
);

CREATE INDEX geo_regions_region ON geo_regions USING SPGIST(region);