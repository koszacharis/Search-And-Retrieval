USE ad;
CREATE TABLE IF NOT EXISTS ItemLocation (ItemID varchar(50) NOT NULL,
						   Location POINT NOT NULL,
						   SPATIAL INDEX(Location),
						   PRIMARY KEY (ItemID, Location),
						   FOREIGN KEY (ItemID) REFERENCES item_coordinates(ItemID)
						   ) ENGINE=MyISAM;

INSERT INTO ItemLocation (ItemID, Location)
SELECT item_id, POINT(latitude, longitude) 
FROM item_coordinates
WHERE latitude IS NOT NULL AND longitude IS NOT NULL;
