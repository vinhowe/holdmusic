from chunk import Chunk
from random import random
from typing import Union, Dict, Optional, List

from quarry.types.buffer import Buffer
from quarry.types.chunk import PackedArray, BlockArray
from quarry.types.nbt import TagRoot, TagCompound, TagLongArray, TagString, TagList
from quarry.types.registry import LookupRegistry
from twisted.internet import reactor
from quarry.net.server import ServerFactory, ServerProtocol
from quarry.types.chat import Message
from quarry.types.uuid import UUID
from datetime import datetime

default_pos = [0.5, 145, 0.5]


class HoldMusicProtocol(ServerProtocol):
    pos = default_pos
    yaw = 0
    pitch = 0
    on_ground = True
    teleport_id = 0
    song_start = None
    music_bar_uuid = None
    playing_music = True
    registry = LookupRegistry.from_json(reports_path="../../reports")
    experience = 1

    def __init__(self, factory, remote_addr):
        super().__init__(factory, remote_addr)
        self.buff_type.registry = self.registry

    def player_joined(self):
        # Call super. This switches us to "play" mode, marks the player as
        #   in-game, and does some logging.
        ServerProtocol.player_joined(self)

        # Send "Join Game" packet
        self.send_packet(
            "join_game",
            self.buff_type.pack(
                "iBqiB",
                1,  # entity id
                2,  # game mode
                1,  # dimension
                0,  # hashed seed
                0,
            ),  # max players
            self.buff_type.pack_string("flat"),  # level type
            self.buff_type.pack_varint(3),  # view distance
            self.buff_type.pack("??", False, True),  # reduced debug info
        )  # show respawn screen

        for x in range(-7, 8):
            for y in range(-7, 8):
                self.send_air_chunk(x, y)

        # self.send_air_chunk(1, 1)

        # Send "Player Position and Look" packet
        self.send_packet(
            "player_position_and_look",
            self.buff_type.pack(
                "dddff?",
                *self.pos,
                self.yaw,
                self.pitch,
                0b00000  # x  # y  # z  # yaw  # pitch
            ),  # flags
            self.buff_type.pack_varint(0),
        )  # teleport id

        # Send "Player Abilities" packet
        self.send_packet(
            "player_abilities", self.buff_type.pack("bff", 0b01111, 0.2, 0.1),
        )

        # Send "Server Difficulty" packet
        self.send_packet(
            "server_difficulty", self.buff_type.pack("B?", 3, False),
        )

        # Send "World Border" packet
        self.send_packet(
            "world_border",
            self.buff_type.pack_varint(0),
            self.buff_type.pack("d", 100),
        )

        # # Send "Server Difficulty" packet
        # self.send_packet(
        #     "chat_message",
        #     self.buff_type.pack_chat(Message({"text": "Playing Stal -- C418", "color": "red"})) + self.buff_type.pack("B", 0),
        # )

        items: List[Optional[str]] = [
            [
                "minecraft:firework_rocket" if random() > 0.5 else "minecraft:pumpkin",
                int(random() * 127),
                None,
            ]
            for _ in range(46)
        ]

        items[36] = [
            "minecraft:written_book",
            1,
            TagRoot(
                {
                    "": TagCompound(
                        {
                            "title": TagString("Server Loading"),
                            "author": TagString("Vin Howe"),
                            "pages": TagList(
                                [
                                    TagString(
                                        "This is a dummy server to keep you distracted while the real server starts running. Hang tight!"
                                    )
                                ]
                            ),
                        }
                    )
                }
            ),
        ]
        # items[6] = "minecraft:elytra"
        # items[36:45] = ["minecraft:firework_rocket" for _ in range(36, 45)]

        self.send_packet(
            "window_items",
            self.buff_type.pack("Bh", 0, 46),
            *[self.buff_type.pack_slot(*item) for item in items]
        )

        self.send_packet("open_book", self.buff_type.pack_varint(0))

        self.update_music(create=True)

        for player in self.factory.players:
            player.send_packet(
                "player_list_item",
                self.buff_type.pack_varint(0),  # action
                self.buff_type.pack_varint(1),  # count
                self.buff_type.pack_uuid(self.uuid),  # uuid
                self.buff_type.pack_string(self.display_name),  # name
                self.buff_type.pack_varint(
                    0
                ),  # properties count (not really sure what to do with this)
                self.buff_type.pack_varint(0),  # gamemode (survival = 0)
                self.buff_type.pack_varint(0),  # ping (in ms)
                self.buff_type.pack("?", False),  # Has Display Name
            )

        # Start sending "Keep Alive" packets
        self.ticker.add_loop(20, self.update_keep_alive)

        self.ticker.add_loop(2, self.send_position_update)

        # Announce player joined
        self.factory.send_chat(u"\u00a7e%s has joined." % self.display_name)

    def player_left(self):
        ServerProtocol.player_left(self)

        for player in self.factory.players:
            player.send_packet(
                "player_list_item",
                self.buff_type.pack_varint(4),  # action
                self.buff_type.pack_varint(1),  # count
                self.buff_type.pack_uuid(self.uuid),  # uuid
            )

        # Announce player left
        self.factory.send_chat(u"\u00a7e%s has left." % self.display_name)

    def update_music(self, create=False):
        if not self.song_start:
            self.song_start = datetime.now()

        percent = (datetime.now() - self.song_start).seconds / ((2 * 60) + 32)

        if create:
            self.music_bar_uuid = UUID.random()
            self.send_packet(
                "boss_bar",
                self.buff_type.pack_uuid(self.music_bar_uuid),
                self.buff_type.pack_varint(0),
                self.buff_type.pack_chat(
                    Message(
                        {
                            "text": "Server is loading -- Playing Stal by C418",
                            "color": "bold",
                        }
                    )
                ),
                self.buff_type.pack("f", percent),
                self.buff_type.pack_varint(1),
                self.buff_type.pack_varint(0),
                self.buff_type.pack("B", 0b00000),
            )

            # Play "Stal" by C418
            self.send_packet(
                "sound_effect",
                self.buff_type.pack_varint(557),
                self.buff_type.pack_varint(2),
                self.buff_type.pack(
                    "iiiff",
                    int(default_pos[0] * 8),
                    int(default_pos[1] * 8),
                    int(default_pos[2] * 8),
                    1,
                    1,
                ),
            )  # teleport id
        elif self.playing_music:
            if percent > 1:
                self.send_packet(
                    "boss_bar",
                    self.buff_type.pack_uuid(self.music_bar_uuid),
                    self.buff_type.pack_varint(2),
                    self.buff_type.pack("f", 1),
                )
                self.send_packet(
                    "boss_bar",
                    self.buff_type.pack_uuid(self.music_bar_uuid),
                    self.buff_type.pack_varint(3),
                    self.buff_type.pack_chat(
                        Message(
                            {
                                "text": "Server is still loading -- hang tight!",
                                "color": "bold",
                            }
                        )
                    ),
                )

                self.playing_music = False
                return
            self.send_packet(
                "boss_bar",
                self.buff_type.pack_uuid(self.music_bar_uuid),
                self.buff_type.pack_varint(2),
                self.buff_type.pack("f", percent),
            )

    def update_keep_alive(self):
        # Send a "Keep Alive" packet

        # 1.7.x
        if self.protocol_version <= 338:
            payload = self.buff_type.pack_varint(0)

        # 1.12.2
        else:
            payload = self.buff_type.pack("Q", 0)

        self.update_music()

        self.send_packet("keep_alive", payload)

    def packet_chat_message(self, buff):
        # When we receive a chat message from the player, ask the factory
        # to relay it to all connected players
        p_text = buff.unpack_string()
        self.factory.send_chat("<%s> %s" % (self.display_name, p_text))

    def send_air_chunk(self, x, z):
        sections = []
        if x == 0 and z == 0:
            sections = [(BlockArray.empty(self.registry), None) for _ in range(16)]
            sections[9][0][0] = {"name": "minecraft:bedrock"}
        sections_data = self.buff_type.pack_chunk(sections)
        motion_world = PackedArray.empty_height()
        motion_blocking = TagLongArray(PackedArray.empty_height())
        world_surface = TagLongArray(PackedArray.empty_height())
        heightmap = TagRoot(
            {
                "": TagCompound(
                    {"MOTION_BLOCKING": motion_blocking, "WORLD_SURFACE": world_surface}
                )
            }
        )
        biomes = [27 for _ in range(1024)]
        block_entities = []
        self.send_packet(
            "chunk_data",
            self.buff_type.pack("ii?", x, z, True),
            self.buff_type.pack_chunk_bitmask(sections),
            self.buff_type.pack_nbt(heightmap),  # added in 1.14
            self.buff_type.pack_array("I", biomes),
            self.buff_type.pack_varint(len(sections_data)),
            sections_data,
            self.buff_type.pack_varint(len(block_entities)),
            b""
            # b"".join(self.buff_type.pack_nbt(entity) for entity in block_entities),
        )

    def packet_player_look(self, buff):
        self.yaw = buff.unpack("f")
        self.pitch = buff.unpack("f")
        self.on_ground = buff.unpack("?")

    def packet_player_movement(self, buff):
        self.on_ground = buff.unpack("?")

    def packet_player_position(self, buff):
        self.pos = list(buff.unpack("ddd"))
        self.on_ground = buff.unpack("?")

    def packet_player_position_and_look(self, buff):
        self.pos = list(buff.unpack("ddd"))
        self.yaw = buff.unpack("f")
        self.pitch = buff.unpack("f")
        self.on_ground = buff.unpack("?")

    def send_position_update(self):
        """
        Move back up if gets too low
        """

        self.send_packet(
            "set_experience",
            self.buff_type.pack("f", 1),
            self.buff_type.pack_varint(self.experience),
            self.buff_type.pack_varint(100000),
        )
        self.experience = int(random() * (2 ** 24))

        if (
            -100 <= self.pos[0] <= 100
            and 50 <= self.pos[1] <= 250
            and -100 <= self.pos[2] <= 100
        ):
            return

        self.pos = default_pos

        self.send_packet(
            "player_position_and_look",
            self.buff_type.pack(
                "dddff?",
                *self.pos,
                self.yaw,
                self.pitch,
                0b00000  # x  # y  # z  # yaw  # pitch
            ),  # flags
            self.buff_type.pack_varint(self.teleport_id),
        )
        # self.teleport_id += 1


class HoldMusicFactory(ServerFactory):
    protocol = HoldMusicProtocol
    motd = "HoldMusic holding room"

    def send_chat(self, message):
        for player in self.players:
            player.send_packet(
                "chat_message",
                player.buff_type.pack_chat(message) + player.buff_type.pack("B", 0),
            )


def main(argv):
    # Parse options
    import argparse

    parser = argparse.ArgumentParser()
    parser.add_argument("-a", "--host", default="", help="address to listen on")
    parser.add_argument(
        "-p", "--port", default=25577, type=int, help="port to listen on"
    )
    args = parser.parse_args(argv)

    # Create factory
    factory = HoldMusicFactory()
    factory.online_mode = False

    # Listen
    factory.listen(args.host, args.port)
    reactor.run()


if __name__ == "__main__":
    import sys

    main(sys.argv[1:])
