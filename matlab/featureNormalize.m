function [X_norm, mu, sigma] = featureNormalize(X, mu, sigma)

X_norm = X;
if (~exist('mu', 'var'))
    mu = zeros(1, size(X, 2));
    % set mu
    mu = mean(X);   % dimens = 1 x 34
end

if (~exist('sigma', 'var'))
    sigma = zeros(1, size(X, 2));
    % set sigma
    sigma = std(X); % dimens = 1 x 34
end

% check sigma
% for i = 1:length(sigma)
%     divider = sigma(i);
%     if divider == 0
%         % sigma 数据为0 说明整列数据都是相同的
%         % 通过将均值设为0, 将整列数据规整为 1
%         mu(i) = 0;
%         sigma(i) = max(X(:, i));
%     end
% end

for i = 1:size(X, 1)
    for j = 1:size(X, 2)
        divider = sigma(1, j);

        % 如果经过上面规整后, sigma 还有 0 的情况
        % 说明整列数据本来就是 0
        if divider ~= 0 
            x_t = (X_norm(i, j) - mu(1, j));
            X_norm(i, j) = x_t / divider;
        end
    end
end

end