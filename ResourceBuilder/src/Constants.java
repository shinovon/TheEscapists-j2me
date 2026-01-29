
public interface Constants {
	
	static final int TILE_SIZE = 16;
	
	// layers enum
	static final int LAYER_GROUND = 0,
			LAYER_VENT = 1,
			LAYER_ROOF = 2,
			LAYER_UNDERGROUND = 3;

	// zones enum
	static final int ZONE_PLAYER_CELL = 1,
			ZONE_INMATE_CELL = 2,
			ZONE_ROLLCALL = 3,
			ZONE_CANTEEN = 4,
			ZONE_SHOWER = 5,
			ZONE_GYM = 6,
			ZONE_LAUNDRY = 7,
			ZONE_KITCHEN = 8,
			ZONE_WOODSHOP = 9,
			ZONE_METALSHOP = 10,
			ZONE_TAILORSHOP = 11,
			ZONE_DELIVERIES = 12,
			ZONE_JANITOR = 13,
			ZONE_GARDENING = 14;

	// jobs enum
	static final int JOB_UNEMPLOYED = 0,
			JOB_LAUNDRY = 1,
			JOB_GARDENING = 2,
			JOB_JANITOR = 3,
			JOB_WOODSHOP = 4,
			JOB_METALSHOP = 5,
			JOB_KITCHEN = 6,
			JOB_DELIVERIES = 7,
			JOB_TAILOR = 8,
			JOB_MAILMAN = 9,
			JOB_LIBRARY = 10;

	static final int COUNT_JOBS = 11;

	static final int MAX_JOB_QUOTA = 20;
	static final int NPC_JOB_QUOTA_THRESHOLD = 10;
	static final int JOB_EXISTING_BIT = 1;
	static final int JOB_OCCUPIED_BIT = 2;
	
	static final byte COLL_NONE = 0;
	static final byte COLL_SOLID = 2;
	static final byte COLL_SOLID_TRANSPARENT = 3;

}
