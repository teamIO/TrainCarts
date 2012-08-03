package com.bergerkiller.bukkit.tc.signactions;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import com.bergerkiller.bukkit.common.utils.StringUtil;
import com.bergerkiller.bukkit.tc.Permission;
import com.bergerkiller.bukkit.tc.TicketMode;
import com.bergerkiller.bukkit.tc.TrainCarts;
import com.bergerkiller.bukkit.tc.controller.MinecartMember;
import com.bergerkiller.bukkit.tc.events.SignActionEvent;
import com.bergerkiller.bukkit.tc.events.SignChangeActionEvent;
import com.earth2me.essentials.register.payment.*;
import com.earth2me.essentials.register.payment.Method.MethodAccount;

/**
 * @author reeZZer
 * Me (Bergerkiller) gives a big thank-you to him for writing the economics for this feature :)
 */
public class SignActionTicket extends SignAction {

	@Override
	public void execute(SignActionEvent info) {
		if (!info.isType("ticket")) return;
		if (!TrainCarts.EssentialsEnabled) return;
		final boolean isTrain;
		if (info.isCartSign() && info.isAction(SignActionType.MEMBER_ENTER, SignActionType.REDSTONE_ON)) {
			isTrain = false;
		} else if (info.isTrainSign() && info.isAction(SignActionType.GROUP_ENTER, SignActionType.REDSTONE_ON)) {
			isTrain = true;
		} else {
			return;
		}
		if ((info.hasMember() && info.isPowered())) {
			Method method = getEconomicManager();
			if (method != null) {
				double money;
				if (info.getLine(3).isEmpty()) {
					money = 30.0;
				} else {
					money = StringUtil.tryParse(info.getLine(3), 0.0);
				}
				TicketMode mode = TicketMode.parse(info.getLine(2));
				List<MinecartMember> members;
				if (isTrain) {
					members = info.getGroup();
				} else {
					members = new ArrayList<MinecartMember>(1);
					members.add(info.getMember());
				}
				for (MinecartMember member : members) {
					if (!member.hasPlayerPassenger()) continue;
					Player player = (Player) member.getPassenger();
					if (!method.hasAccount(player.getName())) continue;
					//actually perform something with it here
					MethodAccount account = method.getAccount(player.getName());
					
					if (mode == TicketMode.ADD) {
						account.add(money);
						player.sendMessage(ChatColor.WHITE + "[*iG*~Ticket System]" + ChatColor.YELLOW + " You received " + money + " Dollars in your bank account!");
					} else if (mode == TicketMode.CHECK) {
						double getbal = account.balance();
						player.sendMessage(ChatColor.WHITE + "[*iG*~Ticket System]" + ChatColor.YELLOW + " You currently have " + getbal + " Dollars  in your bank account!");
					} else if (mode == TicketMode.BUY) {
						if (account.hasUnder(money)) {
							player.sendMessage(ChatColor.WHITE + "[*iG*~Ticket System]" + ChatColor.RED + " You can't afford a Ticket for " + money + " Dollars, sorry.");
							member.eject();
						} else {
							account.subtract(money);
							player.sendMessage(ChatColor.WHITE + "[*iG*~Ticket System]" + ChatColor.YELLOW + " You bought a Ticket for " + money + " Dollars.");
						}
					}
						
				}
			}
		}
	}

	@Override
	public boolean build(SignChangeActionEvent event) {
		if (event.getMode() != SignActionMode.NONE) {
			if (event.isType("ticket")) {
				return handleBuild(event, Permission.BUILD_TICKET, "ticket system", "charges the passengers of a train");
			}
		}
		return false;
	}

	public Method getEconomicManager() {
		return Methods.hasMethod() ? Methods.getMethod() : null;
	}
}