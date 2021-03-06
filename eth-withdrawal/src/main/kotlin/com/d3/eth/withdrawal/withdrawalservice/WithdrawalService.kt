/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.eth.withdrawal.withdrawalservice

import com.d3.commons.sidechain.SideChainEvent
import com.github.kittinunf.result.Result

/**
 * Withdrawal service is responsible for the withdrawal and rollback proof forming and interconnection with Ethereum.
 * It observers Iroha for following events:
 * 1 - transfer to master account - initiates withdrawal case
 * 2 - absence of transfer to user in deposit batch - initiates rollback case
 */
interface WithdrawalService {

    /**
     * Handle Iroha events.
     */
    fun onIrohaEvent(irohaEvent: SideChainEvent.IrohaEvent): Result<List<WithdrawalServiceOutputEvent>, Exception>

    /**
     * Events emitted by [WithdrawalService]
     */
    fun output(): io.reactivex.Observable<Result<List<WithdrawalServiceOutputEvent>, Exception>>

    /**
     * Behavior in case of rollback failure
     */
    fun returnIrohaAssets(event: WithdrawalServiceOutputEvent): Result<Unit, Exception>

    /**
     * Finalize withdrawal
     *
     * @param event - withdrawal event
     * @result hash of finalization transaction in Iroha
     */
    fun finalizeWithdrawal(event: WithdrawalServiceOutputEvent): Result<String, Exception>
}
