package com.dinogo.sysmsg.service;

import java.util.List;

import com.dinogo.sysmsg.dto.request.template.SellerTemplateApplyRequest;
import com.dinogo.sysmsg.dto.request.template.SellerTemplateCreateRequest;
import com.dinogo.sysmsg.dto.request.template.SendTemplateUpdateRequest;
import com.dinogo.sysmsg.dto.request.template.SysTemplateApplyRequest;
import com.dinogo.sysmsg.dto.request.template.SysTemplateCreateRequest;
import com.dinogo.sysmsg.dto.response.SendResponse;
import com.dinogo.sysmsg.dto.response.SendTemplateResponse;

public interface TemplateService {
    SendTemplateResponse createSellerTemplate(SellerTemplateCreateRequest request, Integer loginMemberId);
    SendTemplateResponse createSystemTemplate(SysTemplateCreateRequest request, Integer loginMemberId);
    SendTemplateResponse updateTemplate(Integer sendId, SendTemplateUpdateRequest request, Integer loginMemberId);
    SendResponse applySystemTemplate(SysTemplateApplyRequest request, Integer loginMemberId);
    SendResponse applySellerTemplate(SellerTemplateApplyRequest request, Integer loginMemberId);
    List<SendTemplateResponse> findTemplates(Integer loginMemberId);
}
