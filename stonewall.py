"""
Empty server that send the _bare_ minimum data to keep a minecraft client connected
"""
from argparse import ArgumentParser
from twisted.internet import reactor
from quarry.net.server import ServerFactory, ServerProtocol

class StoneWallProtocol(ServerProtocol):
	def player_joined(self):
		super().player_joined()

		# Sent init packets
		self.send_packet("join_game",
			self.buff_type.pack("iBiB",
					0,
					3,
					1,
					0),
			self.buff_type.pack_string("flat"),
			self.buff_type.pack_varint(1),
			self.buff_type.pack("?", False))

		self.send_packet("player_position_and_look",
			self.buff_type.pack("dddff?",
				0,
				255,
				0,
				0,
				0,
				0b00000),
			self.buff_type.pack_varint(0))

		self.ticker.add_loop(20, self.send_keep_alive) # Keep alive packets

	def send_keep_alive(self):
		self.send_packet("keep_alive", self.buff_type.pack("Q", 0))

if __name__ == "__main__":
	parser = ArgumentParser()
	parser.add_argument("-a", "--host", default="127.0.0.1", help="bind address")
	parser.add_argument("-p", "--port", default=25565, type=int, help="bind port")
	args = parser.parse_args()

	factory = ServerFactory()
	factory.protocol = StoneWallProtocol
	factory.motd = "Stonewall Server"
	factory.online_mode = False

	factory.listen(args.host, args.port)
	reactor.run()
