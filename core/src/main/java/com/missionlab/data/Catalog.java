package com.missionlab.data;

import com.missionlab.model.BuildEvent;
import com.missionlab.model.EventEffect;
import com.missionlab.model.FlightNetwork;
import com.missionlab.model.GroundStation;
import com.missionlab.model.GroundStation.Site;
import com.missionlab.model.Mission;
import com.missionlab.model.Orbit;
import com.missionlab.model.Part;
import com.missionlab.model.Route;
import com.missionlab.model.Slot;
import com.missionlab.model.Transfer;

import java.util.ArrayList;
import java.util.List;

/**
 * ALL GAME CONTENT LIVES HERE: parts, orbits, launch vehicles and flight graph, ground stations, missions and test problems.
 * Nothing else needs to change to tweak numbers or text. After any change,
 * run the balance check (see README) to confirm there are still several good solutions.
 *
 * Units: kg, U (1U = 10 cm cube), watts (W), watt-hours (Wh), thousands of dollars (k), months, GB per day, Mbps.
 * The numbers are simplified for teaching, but keep the order of magnitude of real missions:
 * - CubeSat structures of up to 2 kg per U (CubeSat Design Specification rev. 14.1, Cal Poly);
 * - free deployment from the ISS through NASA's CubeSat Launch Initiative (CSLI);
 * - rideshare launches to sun-synchronous orbit priced per kilogram (like SpaceX Transporter missions);
 * - polar ground stations such as Svalbard, which see a polar satellite on almost every orbit.
 */
public final class Catalog {
    private Catalog() {}

    // ---------------------------------------------------------------- engineering constants
    /** On-board computer, receiver and heaters: always on. */
    public static final double BUS_POWER_W = 2.5;
    /** Volume taken by the flight computer and the power board (every satellite has them). */
    public static final double BUS_VOLUME_U = 0.5;
    /** Flight computer, power board, and vibration and thermal-vacuum testing: every satellite pays for these. */
    public static final double BUS_AND_TESTING_K = 150;
    /** Months of integration and testing after the last part arrives. */
    public static final double INTEGRATION_MONTHS = 3;
    /** Battery depth of discharge per orbit: going deeper wears it out within months (thousands of cycles per year). */
    public static final double BATTERY_DEPTH_OF_DISCHARGE = 0.25;
    /** FCC rule (2022): deorbit the satellite no later than 5 years after the end of the mission. */
    public static final double DEBRIS_RULE_YEARS = 5;
    public static final double DRAG_SAIL_KG = 0.25;
    public static final double DRAG_SAIL_K = 20;

    // ---------------------------------------------------------------- parts
    public static final List<Part> PARTS = new ArrayList<>();

    static {
        // Instruments (Scientist's shelf). IFOV in microradians: at 550 km, 50 µrad = 27.5 m per pixel.
        add(Part.of("cam_basic", Slot.INSTRUMENT, "Wide-field camera")
                .text("Small and cheap. Sees a wide area in daylight, but each pixel covers a lot of ground.")
                .mass(0.4).volume(0.3).power(3).cost(40).lead(2).reliability(0.98)
                .camera(50, 0.5, 0.3, false));
        add(Part.of("cam_sharp", Slot.INSTRUMENT, "High-resolution camera")
                .text("A small telescope: tiny details, but lots of data, and its narrow view needs precise pointing.")
                .mass(1.2).volume(1.5).power(6).cost(220).lead(5).reliability(0.95)
                .camera(8, 4.0, 0.9, false));
        add(Part.of("cam_thermal", Slot.INSTRUMENT, "Thermal infrared camera")
                .text("Sees heat, so it finds fires at night. Coarse pixels. Heavy: two people must carry it.")
                .mass(1.4).volume(1.0).power(8).cost(160).lead(7).reliability(0.94).heavy()
                .camera(90, 1.0, 0.35, true));

        // Power (Engineer's shelves). Watts while in sunlight.
        add(Part.of("panels_body", Slot.POWER, "Body-mounted cells")
                .text("Solar cells glued to the sides. Cannot jam, but give little power.")
                .mass(0.3).cost(30).lead(1).reliability(0.99).generates(8));
        add(Part.of("panels_mid", Slot.POWER, "Small deployable panels")
                .text("Two panels that unfold after launch. A good middle choice.")
                .mass(0.6).volume(0.1).cost(70).lead(3).reliability(0.96).generates(20).deployable());
        add(Part.of("panels_fold", Slot.POWER, "Large solar wings")
                .text("Big wings with lots of power, but their hinges can jam. Heavy: two people must carry them.")
                .mass(1.0).volume(0.2).cost(120).lead(4).reliability(0.93).heavy().generates(36).deployable());
        add(Part.of("batt_small", Slot.BATTERY, "Small battery (15 Wh)")
                .text("Light and cheap. Enough for the computer through each eclipse.")
                .mass(0.2).volume(0.2).cost(8).lead(1).reliability(0.98).stores(15));
        add(Part.of("batt_large", Slot.BATTERY, "Large battery (60 Wh)")
                .text("Keeps a camera running in the dark. Heavy: two people must carry it.")
                .mass(0.5).volume(0.4).cost(22).lead(2).reliability(0.97).heavy().stores(60));
        add(Part.of("point_mag", Slot.POINTING, "Magnetorquers")
                .text("Turn the satellite with Earth's magnetic field. Cheap, but only roughly aimed.")
                .mass(0.2).volume(0.2).power(0.6).cost(25).lead(2).reliability(0.99).stability(0.4));
        add(Part.of("point_wheels", Slot.POINTING, "Reaction wheels + star tracker")
                .text("Spinning wheels steered by a star camera. Aim precisely, but cost a lot and use power.")
                .mass(0.9).volume(0.6).power(3).cost(130).lead(4).reliability(0.95).stability(0.95));
        add(Part.of("frame_2u", Slot.STRUCTURE, "2U frame")
                .text("Two 10 cm cubes: 2U of space, up to 4 kg in total. Cheapest to launch.")
                .mass(0.25).cost(8).lead(1).reliability(0.99).frame(2));
        add(Part.of("frame_3u", Slot.STRUCTURE, "3U frame")
                .text("Three cubes: 3U of space, up to 6 kg. The most common CubeSat size.")
                .mass(0.4).cost(12).lead(1).reliability(0.99).frame(3));
        add(Part.of("frame_6u", Slot.STRUCTURE, "6U frame")
                .text("Six cubes: room for a telescope. Up to 12 kg. Heavy: two people must carry it.")
                .mass(0.9).cost(25).lead(2).reliability(0.98).heavy().frame(6));

        // Radios (Ground control's shelf). Power draw while transmitting.
        add(Part.of("radio_s", Slot.RADIO, "S-band radio (2 Mbps)")
                .text("Small and cheap. Fine for small pictures and many ground stations.")
                .mass(0.15).volume(0.2).power(4).cost(30).lead(2).reliability(0.98).downlink(2));
        add(Part.of("radio_s_fast", Slot.RADIO, "Fast S-band radio (8 Mbps)")
                .text("Four times faster. A good middle choice for speed, power and price.")
                .mass(0.25).volume(0.25).power(8).cost(60).lead(3).reliability(0.97).downlink(8));
        add(Part.of("radio_x", Slot.RADIO, "X-band radio (50 Mbps)")
                .text("Sends big pictures in a single pass. Power-hungry and slow to buy.")
                .mass(0.4).volume(0.3).power(15).cost(120).lead(5).reliability(0.94).downlink(50));
    }

    private static void add(Part.Builder b) {
        PARTS.add(b.build());
    }

    // ---------------------------------------------------------------- orbits
    // All of them cross the equator northbound at 22:30 local time (and southbound at 10:30):
    // morning passes for daylight photos and night passes to look for fire.
    public static final Orbit ISS_ORBIT = new Orbit("orbit_iss", "ISS orbit (420 km)",
            "Where CubeSats are released from the ISS. Low, so photos are sharp, but it never goes past 52 degrees north or south. Air drag brings it down in about 1.5 years.",
            420, 51.6, 22.5, 1.5, false);
    public static final Orbit SSO_550 = new Orbit("orbit_sso550", "Sun-synchronous (550 km)",
            "Passes near both poles and crosses each place at the same local time every day. Sees the whole planet. Lasts about 6 years.",
            550, 97.6, 22.5, 6, true);
    public static final Orbit SSO_800 = new Orbit("orbit_sso800", "High sun-synchronous (800 km)",
            "Sees farther, so ground stations hear it for longer, but photos are less detailed. It would stay up for decades, so it needs a drag sail.",
            800, 98.6, 22.5, 60, true);
    public static final List<Orbit> ORBITS = List.of(ISS_ORBIT, SSO_550, SSO_800);
    /** Orbit used for the hangar's budgets while no orbit has been chosen. */
    public static final Orbit REFERENCE_ORBIT = SSO_550;

    // ---------------------------------------------------------------- launch vehicles (edge weights)
    public static final Transfer CSLI = new Transfer("csli", "NASA CubeSat Launch Initiative", "NASA CSLI",
            "Free ride for schools: the satellite goes to the ISS as cargo and an astronaut releases it. Up to 6 kg, delivered 4 months early.",
            0, 0, 6, 720, 4);
    public static final Transfer NANORACKS = new Transfer("iss_deploy", "Paid ISS deployment", "Paid ISS",
            "A commercial seat in the ISS CubeSat deployer. Takes bigger satellites. Delivered 3 months early.",
            60, 25, 12, 720, 3);
    public static final Transfer RIDE_SSO = new Transfer("ride_sso", "Rideshare to 550 km", "Rideshare",
            "A seat on a rocket carrying dozens of small satellites to sun-synchronous orbit. Priced by the kilo.",
            50, 30, 24, 1.5, 2);
    public static final Transfer RIDE_SSO_HIGH = new Transfer("ride_sso_high", "Rideshare to 800 km", "High rideshare",
            "Fewer rockets go this high, so the base price is higher and you hand over 3 months early.",
            130, 30, 24, 1.5, 3);
    public static final Transfer SPACE_TUG = new Transfer("tug", "Space tug", "Space tug",
            "A small robot ship that picks you up at 550 km and pushes you to 800 km. Takes a month.",
            40, 15, 12, 720, 0);

    // ---------------------------------------------------------------- flight graph
    /**
     * Which vehicle goes from where to where. Each line is a directed edge of the graph.
     * To add a new trip, add a line; the rocket console and the balance check pick it up automatically.
     */
    public static final FlightNetwork FLIGHTS = new FlightNetwork(ORBITS)
            .launch(CSLI, ISS_ORBIT)
            .launch(NANORACKS, ISS_ORBIT)
            .launch(RIDE_SSO, SSO_550)
            .launch(RIDE_SSO_HIGH, SSO_800)
            .transfer(SPACE_TUG, SSO_550, SSO_800);

    // ---------------------------------------------------------------- ground stations
    private static final Site CAL_POLY = new Site("Cal Poly", 35.3, -120.7);
    private static final Site SVALBARD = new Site("Svalbard", 78.2, 15.4);
    private static final Site FAIRBANKS = new Site("Fairbanks", 64.8, -147.7);
    private static final Site WALLOPS = new Site("Wallops", 37.9, -75.5);
    private static final Site HARTEBEESTHOEK = new Site("Hartebeesthoek", -25.9, 27.7);

    public static final List<GroundStation> STATIONS = List.of(
            new GroundStation("gs_uni", "University antenna",
                    "One antenna at Cal Poly, where the CubeSat standard was invented. Cheap, but it hears you only a few times a day.",
                    5, CAL_POLY),
            new GroundStation("gs_polar", "Polar pair",
                    "Svalbard and Fairbanks, near the North Pole. They hear polar satellites on most orbits.",
                    35, SVALBARD, FAIRBANKS),
            new GroundStation("gs_global", "Global network",
                    "Four antennas around the world. The most contact time, at the highest yearly price.",
                    70, SVALBARD, FAIRBANKS, WALLOPS, HARTEBEESTHOEK));

    // ---------------------------------------------------------------- missions (same structure for both)
    public static final Mission AMAZON_FIRE = Mission.builder("amazon_fire", 1, "Amazon Fire Alarm")
            .brief("Fire season is coming. Firefighters in the Amazon need to know where new fires start, and fires are easiest "
                    + "to spot at night, when the forest is cool and the flames glow in infrared. Build a satellite that finds "
                    + "the fires after dark and sends the picture home.")
            .target("Amazon rainforest", -8, -62)
            .region(-14, 2, -74, -50)
            .atNight()
            .budget(700).launchInMonths(14).lasts(1)
            .resolution(100, "to guide firefighters to the right valley")
            .season(8)
            .photo("amazon_fire.png", "PLACEHOLDER - replace with a real NASA night or fire image of the Amazon and its credit line")
            .similarTo("VIIRS on Suomi NPP and NOAA-20, NASA and NOAA instruments that map fires every night")
            .facts("NASA's VIIRS instrument finds fires with 375 m pixels from sun-synchronous orbit, crossing the equator around 1:30 am and 1:30 pm.",
                    "At night the cool forest is dark in thermal infrared, so hot flames stand out clearly.",
                    "NASA's CubeSat Launch Initiative gives schools free rides to space; most of those satellites are released from the ISS.")
            .build();

    public static final Mission GREENLAND = Mission.builder("greenland", 2, "Greenland Ice Watch")
            .brief("Greenland's glaciers are shrinking. Scientists need detailed daylight pictures of where the ice meets the sea, "
                    + "to measure how far each glacier front retreats every summer. Jakobshavn, Greenland's fastest glacier, "
                    + "is the main target.")
            .target("Greenland", 69.2, -50)
            .region(60, 82, -72, -20)
            .budget(1100).launchInMonths(12).lasts(3)
            .resolution(15, "to see where the glacier front breaks into icebergs")
            .season(20)
            .photo("greenland.png", "PLACEHOLDER - replace with a real NASA image of Greenland and its credit line")
            .similarTo("Landsat 8 and 9 (NASA and USGS), which map Greenland's glacier fronts in 15 to 30 m detail")
            .facts("Landsat has photographed Greenland since 1972. Jakobshavn's front retreated more than 40 km between 1850 and 2010.",
                    "Greenland lies between 60 and 83 degrees north, so only polar orbits fly over it. In summer the Sun barely sets.",
                    "Svalbard's ground station, at 78 degrees north, can talk to a polar satellite on almost every orbit.")
            .build();

    /** In campaign order: Mission 1, Mission 2. */
    public static final List<Mission> MISSIONS = List.of(AMAZON_FIRE, GREENLAND);

    // ---------------------------------------------------------------- problems during testing (CubeSat 101)
    public static final List<BuildEvent> EVENTS = List.of(
            new BuildEvent("ev_antenna", "Vibration test: loose antenna",
                    "During the shake test that copies a rocket launch, the antenna came loose.",
                    "Fix and re-test ($15k, 1 month)", "Fly as it is (30% chance it breaks)",
                    15, 1, EventEffect.NO_CONTACT, 0.30, 0, null),
            new BuildEvent("ev_focus", "Vacuum test: camera out of focus",
                    "In the chamber that copies space, the cold pulled the camera lens out of focus.",
                    "Refocus it ($20k, 1 month)", "Fly as it is (40% chance of blurry photos)",
                    20, 1, EventEffect.BLUR, 0.40, 0, null),
            new BuildEvent("ev_supplier", "Supplier is late",
                    "The company building your circuit boards is behind schedule.",
                    "Pay for express delivery ($25k)", "Wait for them (+2 months)",
                    25, 0, EventEffect.DELAY, 1.0, 2, null),
            new BuildEvent("ev_software", "Bug in the flight software",
                    "Testing found a bug that can make the onboard computer restart by itself.",
                    "Fix the code ($10k, 1 month)", "Fly as it is (30% chance of restarts)",
                    10, 1, EventEffect.RESETS, 0.30, 0, null),
            new BuildEvent("ev_hinge", "Stiff solar panel hinge",
                    "One deployable panel opened slowly in the deployment test.",
                    "Replace the hinge ($10k, 0.5 months)", "Fly as it is (35% chance it jams)",
                    10, 0.5, EventEffect.PANEL_JAM, 0.35, 0, "panels_mid|panels_fold"));

    // ---------------------------------------------------------------- helpers
    public static List<Part> partsFor(Slot slot) {
        List<Part> out = new ArrayList<>();
        for (Part p : PARTS) if (p.slot == slot) out.add(p);
        return out;
    }

    public static Part part(String id) {
        for (Part p : PARTS) if (p.id.equals(id)) return p;
        throw new IllegalArgumentException("Unknown part: " + id);
    }

    /** A specific route to an orbit, given by its vehicle ids: route(SSO_800, "ride_sso", "tug"). */
    public static Route route(Orbit to, String... vehicleIds) {
        return FLIGHTS.route(to, vehicleIds);
    }

    public static GroundStation station(String id) {
        for (GroundStation s : STATIONS) if (s.id.equals(id)) return s;
        throw new IllegalArgumentException("Unknown station: " + id);
    }

    public static Mission mission(String id) {
        for (Mission m : MISSIONS) if (m.id.equals(id)) return m;
        throw new IllegalArgumentException("Unknown mission: " + id);
    }
}
