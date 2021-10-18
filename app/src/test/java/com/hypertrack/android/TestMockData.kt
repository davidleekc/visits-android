package com.hypertrack.android

object TestMockData {

    val MOCK_TRIPS_JSON = """
            {
               "pagination_token" : null,
               "links" : {},
               "data" : [
                  {
                     "estimate" : {
                        "arrive_at" : "2020-09-15T09:28:38.176257Z",
                        "route" : {
                           "start_address" : "Sobornyi Ave, 196, Zaporizhzhia, Zaporiz'ka oblast, Ukraine, 69000",
                           "end_address" : "Mykhayla Honcharenka Street, 9, Zaporizhzhia, Zaporiz'ka oblast, Ukraine, 69061",
                           "duration" : 162,
                           "polyline" : {
                              "type" : "LineString",
                              "coordinates" : [
                                 [
                                    35.12218,
                                    47.84857
                                 ],
                                 [
                                    35.1237,
                                    47.84984
                                 ],
                                 [
                                    35.12407,
                                    47.85013
                                 ],
                                 [
                                    35.12415,
                                    47.85009
                                 ],
                                 [
                                    35.12512,
                                    47.84959
                                 ],
                                 [
                                    35.12544,
                                    47.8494
                                 ],
                                 [
                                    35.12483,
                                    47.84887
                                 ],
                                 [
                                    35.12445,
                                    47.84856
                                 ],
                                 [
                                    35.12416,
                                    47.84833
                                 ],
                                 [
                                    35.12363,
                                    47.84791
                                 ]
                              ]
                           },
                           "distance" : 569,
                           "remaining_duration" : 162
                        },
                        "reroutes_exceeded" : false
                     },
                     "eta_relevance_data" : {
                        "status" : true
                     },
                     "device_id" : "42",
                     "device_info" : {
                        "os_version" : "9",
                        "sdk_version" : "4.6.0-SNAPSHOT"
                     },
                     "status" : "active",
                     "completed_at" : null,
                     "analytics" : {},
                     "summary" : null,
                     "trip_id" : "4ee5713c-250e-4094-b6c6-7c7ae33717b6",
                     "views" : {
                        "embed_url" : "https://embed.hypertrack.com/trips/4ee5713c-250e-4094-b6c6-7c7ae33717b6?publishable_key=uvIAA8xJANxUxDgINOX62-LINLuLeymS6JbGieJ9PegAPITcr9fgUpROpfSMdL9kv-qFjl17NeAuBHse8Qu9sw",
                        "share_url" : "https://trck.at/mmmntkgzcj"
                     },
                     "started_at" : "2020-09-15T07:54:02.305516Z",
                     "destination" : {
                        "geometry" : {
                           "type" : "Point",
                           "coordinates" : [
                              35.1235317438841,
                              47.847959440945
                           ]
                        },
                        "address" : "Mykhayla Honcharenka Street, 9, Zaporizhzhia, Zaporiz'ka oblast, Ukraine, 69061",
                        "scheduled_at" : null,
                        "radius" : 30
                     }
                  },
                  {
                     "device_id" : "42",
                     "eta_relevance_data" : {
                        "status" : true
                     },
                     "estimate" : {
                        "arrive_at" : "2020-09-15T09:12:23.584444Z",
                        "route" : {
                           "polyline" : {
                              "type" : "LineString",
                              "coordinates" : [
                                 [
                                    -122.50384,
                                    37.761
                                 ],
                                 [
                                    -122.50393,
                                    37.76239
                                 ],
                                 [
                                    -122.50428,
                                    37.76237
                                 ],
                                 [
                                    -122.50457,
                                    37.76235
                                 ],
                                 [
                                    -122.50607,
                                    37.76229
                                 ],
                                 [
                                    -122.50822,
                                    37.7622
                                 ],
                                 [
                                    -122.50965,
                                    37.76213
                                 ],
                                 [
                                    -122.50966,
                                    37.76221
                                 ]
                              ]
                           },
                           "distance" : 668,
                           "remaining_duration" : 137,
                           "start_address" : "1374 44th Ave, San Francisco, CA 94122, USA",
                           "end_address" : "1300 Great Hwy, San Francisco, CA 94122, USA",
                           "duration" : 137
                        },
                        "reroutes_exceeded" : false
                     },
                     "completed_at" : null,
                     "analytics" : {},
                     "device_info" : {
                        "sdk_version" : "4.4.0",
                        "os_version" : "13.5.1"
                     },
                     "status" : "active",
                     "summary" : null,
                     "views" : {
                        "embed_url" : "https://embed.hypertrack.com/trips/6f6d89eb-6b0e-444f-bf32-8601d488c69b?publishable_key=uvIAA8xJANxUxDgINOX62-LINLuLeymS6JbGieJ9PegAPITcr9fgUpROpfSMdL9kv-qFjl17NeAuBHse8Qu9sw",
                        "share_url" : "https://trck.at/mmmntdawy3"
                     },
                     "trip_id" : "6f6d89eb-6b0e-444f-bf32-8601d488c69b",
                     "destination" : {
                        "radius" : 30,
                        "scheduled_at" : null,
                        "address" : "1300 Great Hwy, San Francisco, CA 94122, USA",
                        "geometry" : {
                           "coordinates" : [
                              -122.509639,
                              37.762207
                           ],
                           "type" : "Point"
                        }
                     },
                     "started_at" : "2020-09-15T07:51:38.828420Z"
                  }
               ]
            }
            """.trimIndent()

    val MOCK_HISTORY_JSON = """
            {
               "markers" : [
                    {
                         "marker_id" : "8b6aeb0f-1a8f-4900-95ee-03755ba21015",
                         "data" : {
                            "value" : "inactive",
                            "end" : {
                               "recorded_at" : "2021-02-05T11:53:10.544Z",
                               "location" : {
                                  "geometry" : {
                                     "coordinates" : [ -122.397368, 37.792382 ],
                                     "type" : "Point"
                                  },
                                  "recorded_at" : "2021-02-05T11:53:10.544Z"
                               }
                            },
                            "start" : {
                               "recorded_at" : "2021-02-05T00:00:00+00:00",
                               "location" : {
                                  "geometry" : {
                                     "type" : "Point",
                                     "coordinates" : [ -122.397368, 37.792382 ]
                                  },
                                  "recorded_at" : "2021-02-05T11:53:10.544Z"
                               }
                            },
                            "duration" : 42791,
                            "reason" : "stopped_programmatically"
                         },
                         "type" : "device_status"
                     },
                     {
                         "data" : {
                            "metadata" : {
                               "type" : "Test geotag at 1612342206755"
                            },
                            "location" : {
                               "type" : "Point",
                               "coordinates" : [ -122.084, 37.421998, 5 ]
                            },
                            "recorded_at" : "2021-02-03T08:50:06.757Z"
                         },
                         "type" : "trip_marker",
                         "marker_id" : "b05df9e8-8f91-44eb-b01f-bacfa59b4349"
                    },
                    {
                        "marker_id" : "5eb13571-d3cc-494d-966e-1cc5759ba965",
                        "type" : "geofence",
                        "data" : {
                           "exit" : {
                              "location" : {
                                 "geometry" : null,
                                 "recorded_at" : "2021-02-05T12:18:20.986Z"
                              }
                           },
                           "duration" : 403,
                           "arrival" : {
                              "location" : {
                                 "geometry" : {
                                    "coordinates" : [-122.4249, 37.7599 ],
                                    "type" : "Point"
                                 },
                                 "recorded_at" : "2021-02-05T12:11:37.838Z"
                              }
                           },
                           "geofence" : {
                              "metadata" : {
                                 "name" : "Mission Dolores Park"
                              },
                              "geometry" : {
                                 "coordinates" : [
                                    -122.426366,
                                    37.761115
                                 ],
                                 "type" : "Point"
                              },
                              "geofence_id" : "8b63f7d3-4ba4-4dbf-b100-0c843445d5b2",
                              "radius" : 200
                           }
                        }
                     }
                ],
               "device_id" : "A24BA1B4-3B11-36F7-8DD7-15D97C3FD912",
               "completed_at" : "2021-02-05T22:00:00.000Z",
               "locations" : {
                  "coordinates" : [],
                  "type" : "LineString"
               },
            "started_at": "2021-02-16T22:00:00.000Z",
            "completed_at": "2021-02-17T22:00:00.000Z",
            "distance": 1007.0,
            "duration": 86400,
            "tracking_rate": 100.0,
            "inactive_reasons": [],
            "inactive_duration": 0,
            "active_duration": 158,
            "stop_duration": 0,
            "drive_duration": 158,
            "walk_duration": 0,
            "trips": 0,
            "geotags": 0,
            "geofences_visited": 0
            }
        """.trimIndent()

    val VISIT_WITH_TRACKING_STOPPED_EXIT = """
            {
            "marker_id": "81a18d2b-1719-419d-b2e6-93e023d2a52b",
            "device_id": "device1",
            "created_at": "2021-07-12T16:11:01.012Z",
            "arrival": {
                "recorded_at": "2021-07-12T16:11:01.012Z",
                "location": {
                    "coordinates": [
                        -122.089475,
                        37.395912,
                        2.52
                    ],
                    "type": "Point"
                }
            },
            "exit": {
                "recorded_at": "2021-08-15T14:59:35.067Z",
                "location": null
            },
            "outage_event": null,
            "geofence_type": "device",
            "geofence_id": "c1aca31a-fef7-45fb-a076-4a16980517fc",
            "metadata": {
                "address": "Mountain View, Mariposa Avenue, 336"
            },
            "geometry": {
                "type": "Point",
                "coordinates": [
                    -122.08947490900756,
                    37.39593753392873
                ]
            },
            "duration": 2933314
        }
        """.trimIndent()

}